package DotsBoxes.player.ai;

import DotsBoxes.board.Action;
import DotsBoxes.board.Board;
import DotsBoxes.observers.AlphaBetaPruningObserver;
import DotsBoxes.observers.NodeCounterObserver;
import DotsBoxes.player.ActionStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import DotsBoxes.observers.TimeOutException;
/**
 * Version etudiants.
 * TODO:
 * - Implementer Minimax avec elagage Alpha-Beta.
 * - Utiliser les observateurs pour compter coupes et noeuds visites.
 * - Tenir compte de la regle de rejeu apres fermeture de boxe.
 */
public class AlphaBetaActionStrategy implements ActionStrategy {
    private final AlphaBetaPruningObserver observer;
    private final NodeCounterObserver nodeCounter;
    private final HeuristicFunction heuristic = new HeuristicFunction();

    private void checkTime(long deadline) {
        if (System.currentTimeMillis()>= deadline) {
            throw new TimeOutException();
        }
    }

    public AlphaBetaActionStrategy(AlphaBetaPruningObserver observer, NodeCounterObserver nodeCounter) {
        this.observer    = observer;
        this.nodeCounter = nodeCounter;
    }

    public int alphaBeta(Board board, int depth, int currentPlayerId,
                         int playerId, int score, int alpha, int beta,long deadline) {
        checkTime(deadline);
        nodeCounter.increment();
        observer.incrementNodeCount();

        List<Action> actions = board.getSortedActions();

        if (actions.isEmpty()) {
            if (score > 0) return 1000000; // Victoire assurée
            if (score < 0) return -1000000;
        }
        if (depth == 0) {
            return heuristic.evaluate(board, playerId);
        }
        boolean isMax = (playerId == currentPlayerId);

        int best;
        if (isMax) {
            best = Integer.MIN_VALUE;

            for (Action action : actions) {
                int nbFerme    = board.apply(action, currentPlayerId);
                int newScore   = score + (Math.max(nbFerme, 0));
                int nextPlayer = (nbFerme > 0) ? currentPlayerId : 1 - currentPlayerId;
                int res;

                int nextDepth = (nbFerme > 0) ? depth : depth - 1;
                try { // pour éviter de quitter la boucle sans undo
                    res = alphaBeta(board, nextDepth, nextPlayer, playerId, newScore, alpha, beta, deadline);
                } finally {
                    board.undo(action);
                }

                best  = Math.max(best, res);
                alpha = Math.max(alpha, best);

                if (alpha >= beta) {
                    observer.incrementAlphaCut();
                    break;
                }

            }

        } else {
            best = Integer.MAX_VALUE;

            for (Action action : actions) {
                int nbFerme    = board.apply(action, currentPlayerId);
                int newScore   = score - (Math.max(nbFerme, 0));
                int nextPlayer = (nbFerme > 0) ? currentPlayerId : 1 - currentPlayerId;
                int nextDepth = (nbFerme > 0) ? depth : depth - 1;
                int res;

                try {// pour éviter de quitter la boucle sans undo
                    res = alphaBeta(board, nextDepth, nextPlayer, playerId, newScore, alpha, beta, deadline);
                } finally {
                    board.undo(action);
                }

                best = Math.min(best, res);
                beta = Math.min(beta, best);

                if (alpha >= beta) {
                    observer.incrementBetaCut();
                    break;
                }
            }
        }
        return best;
    }

    public Action findBestActionAtDepth(Board board, int currentDepth,
                                        List<Action> actionsAvailable, int playerId, long deadline) {
        observer.reset();
        nodeCounter.reset();

        int initialScore = board.getScore(playerId) - board.getScore(1 - playerId);

        List<Action> bestActions = new ArrayList<>();
        int maxVal = Integer.MIN_VALUE;
        int alpha  = Integer.MIN_VALUE;
        int beta   = Integer.MAX_VALUE;

        for (Action action : actionsAvailable) {

            int nbFerme    = board.apply(action, playerId);
            int newScore   = initialScore + nbFerme;
            int nextPlayer = (nbFerme > 0) ? playerId : 1 - playerId;
            int res;

            try { // pour être sur d'annuler les actions avant de stopper
                int nextDepth = (nbFerme > 0) ? currentDepth : currentDepth - 1;
                res = alphaBeta(board, nextDepth, nextPlayer, playerId, newScore, alpha, beta,deadline);
            } finally {
                board.undo(action);
            }
            if (res > maxVal) {
                maxVal = res;
                bestActions.clear();
                bestActions.add(action);
            } else if (res == maxVal) {
                bestActions.add(action);
            }

            alpha = maxVal;
        }


        return bestActions.get(new Random().nextInt(bestActions.size()));
    }

    @Override
    public Action selectAction(Board board, int playerId) {
        long startTime = System.currentTimeMillis();
        long timeLimit = 1000;
        long deadline = startTime+timeLimit;

        Action bestActionFound = null;
        int currentDepth = 1;

        List<Action> actionsAvailable = board.getSortedActions();
        if (actionsAvailable.isEmpty()) return null;

        try {
            do {

                bestActionFound = findBestActionAtDepth(board, currentDepth, actionsAvailable, playerId, deadline);

                currentDepth++;

            } while (currentDepth <= actionsAvailable.size());
        } catch (TimeOutException ignored){}
        observer.printStats();

        System.out.print("stopped at depth :");
        System.out.println(currentDepth);
        //au cas où on test sur de très gros plateau
        return (bestActionFound != null) ? bestActionFound : actionsAvailable.get(new Random().nextInt(actionsAvailable.size()));
    }

    @Override
    public String getName() {
        return "Alpha-Beta";
    }
}