package DotsBoxes.player.ai;

import DotsBoxes.board.Action;
import DotsBoxes.board.Board;
import DotsBoxes.observers.AlphaBetaPruningObserver;
import DotsBoxes.observers.NodeCounterObserver;

public class AlphaBetaBenchmark {

    public static double[] benchmarkGame(int rows, int cols) {
        AlphaBetaPruningObserver observer = new AlphaBetaPruningObserver();
        NodeCounterObserver nodeCounter   = new NodeCounterObserver();
        AlphaBetaActionStrategy ai        = new AlphaBetaActionStrategy(observer, nodeCounter);
        Board board                       = new Board(rows, cols);

        int  currentPlayer  = 0;
        int  totalDecisions = 0;
        long totalTime      = 0;
        long totalNodes     = 0;
        long totalAlphaCuts = 0;
        long totalBetaCuts  = 0;

        while (!board.isFinished()) {
            observer.reset();
            nodeCounter.reset();

            long start    = System.currentTimeMillis();
            Action action = ai.selectAction(board, currentPlayer);
            long end      = System.currentTimeMillis();

            totalTime      += (end - start);
            totalNodes     += observer.getNodeCount();
            totalAlphaCuts += observer.getAlphaCutCount();
            totalBetaCuts  += observer.getBetaCutCount();
            totalDecisions++;

            int nbFerme = board.apply(action, currentPlayer);
            if (nbFerme == 0) currentPlayer = 1 - currentPlayer;
        }

        return new double[]{
                (double) totalTime / totalDecisions,
                totalNodes,
                totalAlphaCuts,
                totalBetaCuts
        };
    }

    public static void main(String[] args) {
        int[][] grids  = {{3,3}, {4,4}, {5,5}};
        int[]   depths = {1, 2, 3, 4, 5};
        int     RUNS   = 3;

        System.out.printf("%-10s %-8s %-22s %-20s %-15s %-15s%n",
                "Grille", "Depth", "Temps moyen/décision", "Noeuds/partie", "Coupes Alpha", "Coupes Beta");
        System.out.println("-".repeat(90));

        for (int[] grid : grids) {
            boolean tropLent = false;
            for (int depth : depths) {
                if (tropLent) break;

                double totalAvgTime  = 0;
                double totalAvgNodes = 0;
                double totalAvgAlpha = 0;
                double totalAvgBeta  = 0;
                boolean cutEarly     = false;

                for (int i = 0; i < RUNS; i++) {
                    double[] results = benchmarkGame(grid[0], grid[1]);

                    totalAvgTime  += results[0];
                    totalAvgNodes += results[1];
                    totalAvgAlpha += results[2];
                    totalAvgBeta  += results[3];

                    if (results[0] > 5000) { tropLent = true; cutEarly = true; break; }
                }

                if (!cutEarly) {
                    System.out.printf("%-10s %-8d %-22s %-20s %-15s %-15s%n",
                            grid[0]+"x"+grid[1],
                            depth,
                            String.format("%.2f ms ✓",   totalAvgTime  / RUNS),
                            String.format("%.0f noeuds", totalAvgNodes / RUNS),
                            String.format("%.0f α-cuts", totalAvgAlpha / RUNS),
                            String.format("%.0f β-cuts", totalAvgBeta  / RUNS));
                } else {
                    System.out.printf("%-10s %-8d %-22s%n",
                            grid[0]+"x"+grid[1], depth, "trop lent, arrêt");
                }
            }
            System.out.println();
        }
    }
}