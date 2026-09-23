package DotsBoxes.player.ai;

import DotsBoxes.board.Action;
import DotsBoxes.board.Board;

public class MinimaxBenchmark {

    // Retourne un tableau : [temps moyen par décision (ms), noeuds totaux, nombre de décisions]
    public static double[] benchmarkGame(int rows, int cols, int depth) {
        Board board = new Board(rows, cols);
        MinimaxActionStrategy ai = new MinimaxActionStrategy(depth);

        int currentPlayer  = 0;
        int totalDecisions = 0;
        long totalTime     = 0;
        long totalNodes    = 0;

        while (!board.isFinished()) {
            ai.getObserver().reset();

            long start  = System.currentTimeMillis();
            Action action = ai.selectAction(board, currentPlayer);
            long end    = System.currentTimeMillis();

            totalTime   += (end - start);
            totalNodes  += ai.getObserver().getCount();
            totalDecisions++;

            int nbFerme = board.apply(action, currentPlayer);
            if (nbFerme == 0) currentPlayer = 1 - currentPlayer;
        }

        return new double[]{
                (double) totalTime  / totalDecisions,  // [0] temps moyen par décision
                totalNodes,                             // [1] noeuds totaux sur la partie
                totalDecisions                          // [2] nombre de décisions
        };
    }

    public static void main(String[] args) {
        int[][] grids  = {{3,3}, {4,4}, {5,5}};
        int[]   depths = {1, 2, 3, 4, 5};
        int     RUNS   = 3;

        System.out.printf("%-10s %-8s %-25s %-20s%n", "Grille", "Depth", "Temps moyen/décision", "Noeuds moyens/partie");
        System.out.println("-".repeat(65));

        for (int[] grid : grids) {
            boolean tropLent = false;
            for (int depth : depths) {
                if (tropLent) break;

                double totalAvgTime  = 0;
                double totalAvgNodes = 0;
                boolean cutEarly     = false;

                for (int i = 0; i < RUNS; i++) {
                    double[] results = benchmarkGame(grid[0], grid[1], depth);
                    double t     = results[0];
                    double nodes = results[1];

                    totalAvgTime  += t;
                    totalAvgNodes += nodes;

                    System.out.printf("  [Run %d] %dx%d depth=%d → %.0f ms | %.0f noeuds%n",
                            i+1, grid[0], grid[1], depth, t, nodes);

                    if (t > 5000) { tropLent = true; cutEarly = true; break; }
                }

                if (!cutEarly) {
                    System.out.printf("%-10s %-8d %-25s %-20s%n",
                            grid[0]+"x"+grid[1],
                            depth,
                            String.format("%.2f ms ✓", totalAvgTime  / RUNS),
                            String.format("%.0f noeuds",  totalAvgNodes / RUNS));
                } else {
                    System.out.printf("%-10s %-8d %-25s%n",
                            grid[0]+"x"+grid[1], depth, "trop lent, arrêt");
                }
            }
            System.out.println();
        }
    }
}