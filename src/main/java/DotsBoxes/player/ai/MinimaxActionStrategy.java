package DotsBoxes.player.ai;

import DotsBoxes.board.Action;
import DotsBoxes.board.Board;
import DotsBoxes.observers.NodeCounterObserver;
import DotsBoxes.player.ActionStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MinimaxActionStrategy implements ActionStrategy {

    private final int maxDepth;
    private final NodeCounterObserver observer = new NodeCounterObserver();
    private final HeuristicFunction heuristic = new HeuristicFunction();

    public MinimaxActionStrategy(int maxDepth) {
        this.maxDepth = maxDepth;
    }

    public int minimax(Board board, int depth, int currentPlayerId,
                       int playerId, int score) {

        observer.increment(); // ← on compte chaque nœud visité

        List<Action> actions = board.getAvailableActions();
        //if (depth == 0) return HeuristicFunction.evaluate(board, playerId, score);
        if (actions.isEmpty() || depth == 0) return score;

        boolean isMax = (playerId == currentPlayerId);
        int best = isMax ? Integer.MIN_VALUE : Integer.MAX_VALUE;

        for (Action action : actions) {
            int nbFerme = board.apply(action, currentPlayerId);

            int newScore = score;
            if (nbFerme > 0) {
                if (currentPlayerId == playerId) newScore += nbFerme;
                else                             newScore -= nbFerme;
            }

            // quand on aura l'élagage alpha beta +intelligent mais trop lent
            // int nextDepth = (nbFerme > 0) ? depth : depth - 1;
            int nextDepth = depth - 1;
            int nextPlayer = (nbFerme > 0) ? currentPlayerId : 1 - currentPlayerId;
            int res = minimax(board, nextDepth, nextPlayer, playerId, newScore);

            if (isMax) best = Math.max(best, res);
            else       best = Math.min(best, res);

            board.undo(action);
        }
        return best;
    }

    @Override
    public Action selectAction(Board board, int playerId) {
        List<Action> actionsAvailable = board.getAvailableActions();
        if (actionsAvailable.isEmpty()) return null;

        observer.reset(); // ← on repart de zéro à chaque décision

        int initialScore = board.getScore(playerId) - board.getScore(1 - playerId);

        List<Action> bestActions = new ArrayList<>();
        int maxVal = Integer.MIN_VALUE;

        for (Action action : actionsAvailable) {
            int nbFerme = board.apply(action, playerId);

            int newScore = initialScore + nbFerme;
            int nextPlayer = (nbFerme > 0) ? playerId : 1 - playerId;

            int res = minimax(board, maxDepth - 1, nextPlayer, playerId, newScore);

            board.undo(action);
            if (res > maxVal) {
                maxVal = res;
                bestActions.clear();
                bestActions.add(action);
            } else if (res == maxVal) {
                bestActions.add(action);
            }
        }

        System.out.println("[Minimax] Noeuds visités : " + observer.getCount());
        return bestActions.get(new Random().nextInt(bestActions.size()));
    }

    public NodeCounterObserver getObserver() {
        return observer;
    }

    @Override
    public String getName() {
        return "Minimax";
    }
}