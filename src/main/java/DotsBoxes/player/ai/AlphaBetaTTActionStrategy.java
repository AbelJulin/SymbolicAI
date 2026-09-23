package DotsBoxes.player.ai;

import DotsBoxes.board.Action;
import DotsBoxes.board.Board;
import DotsBoxes.observers.AlphaBetaPruningObserver;
import DotsBoxes.observers.NodeCounterObserver;
import DotsBoxes.observers.TimeOutException;
import DotsBoxes.player.ActionStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Collections;

/**
 * Stratégie Alpha-Beta avec Table de Transposition (Zobrist) et Move Ordering.
 * L'idée est d'accélérer la recherche en mémorisant les plateaux déjà vus.
 */
public class AlphaBetaTTActionStrategy implements ActionStrategy {

    // Composants pour les stats et la mémoire
    private final AlphaBetaPruningObserver observateur;
    private final NodeCounterObserver compteurNoeuds;
    private final TranspositionTable tableTransposition = new TranspositionTable();

    // Gestion du Hash Zobrist
    private ZobristHash zobrist;
    private int zobristRows = -1;
    private int zobristCols = -1;

    // Variables pour gérer le chrono sur toute la durée de la partie (tournoi)
    private long tempsConsommeTotal = 0;
    private long budgetTotalMs = -1;

    // Pour savoir si on vient de commencer une nouvelle partie
    private int aretesDernierCoup = -1;

    /**
     * Initialisation de l'IA avec les outils de stats pour le débug.
     */
    public AlphaBetaTTActionStrategy(AlphaBetaPruningObserver observateur,
                                     NodeCounterObserver compteurNoeuds) {
        this.observateur     = observateur;
        this.compteurNoeuds  = compteurNoeuds;
    }

    // Méthode pour vérifier si on dépasse le temps alloué
    private void verifierTemps(long deadline) {
        if (System.currentTimeMillis() >= deadline) {
            throw new TimeOutException();
        }
    }

    /**
     * Trie les actions pour tester le meilleur coup connu en premier (Move ordering).
     * Permet de faire plus de coupures Alpha-Beta.
     */
    private List<Action> reordonnerAvecTT(List<Action> actions, long hash) {
        TTEntry entree = tableTransposition.lookup(hash);

        if (entree == null || entree.bestAction == null || !actions.contains(entree.bestAction)) {
            return actions;
        }

        // On place le bestAction en tête s'il est dans la liste
        Action meilleurConnu = entree.bestAction;
        List<Action> reordonnees = new ArrayList<>(actions.size());
        reordonnees.add(meilleurConnu);

        for (Action action : actions) {
            if (!action.equals(meilleurConnu)) {
                reordonnees.add(action);
            }
        }

        return reordonnees;
    }

    // L'algorithme Alpha-Beta principal avec la gestion de la table de transposition
    public int alphaBetaTT(Board board, int depth, int joueurCourant,
                           int joueurPrincipal, int score,
                           int alpha, int beta, long deadline) {

        verifierTemps(deadline);
        compteurNoeuds.increment();
        observateur.incrementNodeCount();
        if (this.zobrist == null) {
            this.zobrist = new ZobristHash(board.getRows(), board.getCols());
        }

        // On regarde si on a déjà ce plateau en mémoire
        long hash = zobrist.computeWithPlayer(board, joueurCourant);
        hash ^= ((long) score * 0x9E3779B97F4A7C15L);
        TTEntry entree = tableTransposition.lookup(hash);

        if (entree != null && entree.depth >= depth) {
            switch (entree.flag) {
                case EXACT:
                    // Valeur exacte — on peut la retourner directement
                    return entree.value;

                case LOWER_BOUND:
                    // Vraie valeur ≥ entree.value — on élève alpha
                    alpha = Math.max(alpha, entree.value);
                    break;

                case UPPER_BOUND:
                    // Vraie valeur ≤ entree.value — on abaisse beta
                    beta = Math.min(beta, entree.value);
                    break;
            }

            // Coupure possible après mise à jour des bornes
            if (alpha >= beta) {
                return entree.value;
            }
        }


        // Cas de base (fin de partie ou profondeur max atteinte)
        List<Action> actions = board.getSortedActions();

        if (actions.isEmpty()) {
            if (score > 0) return 1_000_000 + score;
            if (score < 0) return -1_000_000 + score;
            return 0;
        }

        if (depth == 0) {
            return HeuristicFunction.evaluate(board, joueurPrincipal);
        }

        // Move Ordering pour optimiser l'élagage
        actions = reordonnerAvecTT(actions, hash);

        // Boucle Alpha-Beta classique (Minimax)
        boolean estMax   = (joueurPrincipal == joueurCourant);
        int alphaInitial = alpha;
        int betaInitiale = beta;
        int best;
        Action meilleurCoup = null;

        if (estMax) {
            best = Integer.MIN_VALUE;

            for (Action action : actions) {
                int nbFerme    = board.apply(action, joueurCourant);
                int nouveauScore = score + nbFerme;
                int prochainJoueur = (nbFerme > 0) ? joueurCourant : 1 - joueurCourant;
                int prochaineProf  = (nbFerme > 0) ? depth : depth - 1;
                int res;

                try {
                    res = alphaBetaTT(board, prochaineProf, prochainJoueur,
                            joueurPrincipal, nouveauScore, alpha, beta, deadline);
                } finally {
                    board.undo(action);
                }

                if (res > best) {
                    best        = res;
                    meilleurCoup = action;
                }

                alpha = Math.max(alpha, best);

                if (alpha >= beta) {
                    observateur.incrementAlphaCut();
                    break;
                }
            }

        } else {
            best = Integer.MAX_VALUE;

            for (Action action : actions) {
                int nbFerme    = board.apply(action, joueurCourant);
                int nouveauScore = score - nbFerme;
                int prochainJoueur = (nbFerme > 0) ? joueurCourant : 1 - joueurCourant;
                int prochaineProf  = (nbFerme > 0) ? depth : depth - 1;
                int res;

                try {
                    res = alphaBetaTT(board, prochaineProf, prochainJoueur,
                            joueurPrincipal, nouveauScore, alpha, beta, deadline);
                } finally {
                    board.undo(action);
                }

                if (res < best) {
                    best        = res;
                    meilleurCoup = action;
                }

                beta = Math.min(beta, best);

                if (alpha >= beta) {
                    observateur.incrementBetaCut();
                    break;
                }
            }
        }

        // On enregistre le résultat dans la table avant de remonter
        TTEntry.Flag flag;
        if (best <= alphaInitial) {
            // Coupure alpha : la vraie valeur est ≤ best
            flag = TTEntry.Flag.UPPER_BOUND;
        } else if (best >= betaInitiale) {
            // Coupure beta : la vraie valeur est ≥ best
            flag = TTEntry.Flag.LOWER_BOUND;
        } else {
            // Aucune coupure : valeur exacte
            flag = TTEntry.Flag.EXACT;
        }

        tableTransposition.store(hash, best, depth, flag, meilleurCoup);

        return best;
    }

    // Lance une recherche à une profondeur précise
    public Action rechercherMeilleurCoupAProfondeur(Board board, int profondeur,
                                                    List<Action> actionsDisponibles,
                                                    int joueurPrincipal, long deadline) {
        observateur.reset();
        compteurNoeuds.reset();

        int scoreInitial = board.getScore(joueurPrincipal) - board.getScore(1 - joueurPrincipal);

        Action meilleurCoupChoisi = null;
        int valeurMax  = Integer.MIN_VALUE;
        int alpha      = Integer.MIN_VALUE;
        int beta       = Integer.MAX_VALUE;

        for (Action action : actionsDisponibles) {
            int nbFerme        = board.apply(action, joueurPrincipal);
            int nouveauScore   = scoreInitial + nbFerme;
            int prochainJoueur = (nbFerme > 0) ? joueurPrincipal : 1 - joueurPrincipal;
            int prochaineProf  = (nbFerme > 0) ? profondeur : profondeur - 1;
            int res;

            try {
                res = alphaBetaTT(board, prochaineProf, prochainJoueur,
                        joueurPrincipal, nouveauScore, alpha, beta, deadline);
            } finally {
                board.undo(action);
            }

            // On n'accepte le coup que s'il est strictement meilleur
            if (res > valeurMax) {
                valeurMax = res;
                meilleurCoupChoisi = action;
                alpha = valeurMax;
            }
        }

        // Sécurité anti-crash
        if (meilleurCoupChoisi == null) {
            return actionsDisponibles.get(0);
        }

        return meilleurCoupChoisi;
    }


    @Override
    public Action selectAction(Board board, int playerId) {
        long debut = System.currentTimeMillis();
        int aretesDispo = board.getAvailableActions().size();
        int totalEdges = board.getRows() * (board.getCols() - 1) + (board.getRows() - 1) * board.getCols();

        // Réinitialisation au début de chaque match
        if (aretesDispo > aretesDernierCoup) {
            budgetTotalMs = totalEdges * 1000L;
            tempsConsommeTotal = 0;
            tableTransposition.clear(); // On vide la mémoire de la partie précédente
        }

        // On met à jour pour le prochain tour
        aretesDernierCoup = aretesDispo;

        // Initialisation Zobrist
        if (zobrist == null || board.getRows() != zobristRows || board.getCols() != zobristCols) {
            zobrist = new ZobristHash(board.getRows(), board.getCols());
            zobristRows = board.getRows();
            zobristCols = board.getCols();
        }

        List<Action> actionsDisponibles = board.getSortedActions();
        if (actionsDisponibles.isEmpty()) return null;
        Collections.shuffle(actionsDisponibles);

        // On calcule combien de temps on s'autorise pour ce coup
        long tempsRestant = budgetTotalMs - tempsConsommeTotal;
        long tempsAlloueCeCoup = tempsRestant / (actionsDisponibles.size() / 4 + 1);
        tempsAlloueCeCoup = Math.max(50, Math.min(15000, tempsAlloueCeCoup));
        long deadline = debut + tempsAlloueCeCoup;

        Action meilleurCoupTrouve = null;
        int profondeurCourante    = 1;

        try {
            // On cherche de plus en plus profond (Iterative Deepening)
            do {
                meilleurCoupTrouve = rechercherMeilleurCoupAProfondeur(
                        board, profondeurCourante, actionsDisponibles, playerId, deadline
                );
                profondeurCourante++;

            } while (profondeurCourante <= actionsDisponibles.size());

        } catch (TimeOutException ignored) {}

        // Mise à jour de l'horloge
        tempsConsommeTotal += (System.currentTimeMillis() - debut);

        if (meilleurCoupTrouve == null) {
            // Aléatoire car shuffle avant de chercher meilleurCoupTrouve
            return actionsDisponibles.get(0);
        }

        tableTransposition.printStats();
        return meilleurCoupTrouve;
    }

    // Getters
    public TranspositionTable getTableTransposition() {
        return tableTransposition;
    }

    @Override
    public String getName() {
        return "Alpha-Beta + Table de Transposition";
    }
}
