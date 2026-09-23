package DotsBoxes.player.ai;

import DotsBoxes.board.Action;
import DotsBoxes.board.Board;
import DotsBoxes.observers.AlphaBetaPruningObserver;
import DotsBoxes.observers.NodeCounterObserver;
import DotsBoxes.player.ActionStrategy;
import DotsBoxes.player.automate.GloutonActionStrategy;

/**
 * Arene de test pour comparer deux stratégies IA dans des conditions
 * proches du tournoi officiel.
 * Pour changer les deux IA à confronter, modifier uniquement la section
 * Configuration du match.
 */
public class AIArena {

    // Pour ne pas polluer la console avec les logs des IA pendant les matchs
    private static final java.io.PrintStream STDOUT_ORIGINAL = System.out;
    private static final java.io.PrintStream SILENCE = new java.io.PrintStream(
            java.io.OutputStream.nullOutputStream()
    );

    private static void silence() { System.setOut(SILENCE); }
    private static void parole()  { System.setOut(STDOUT_ORIGINAL); }

    // Stocke les résultats d'une partie (score, temps, timeout)
    private record MatchResult(
            int scoreJ0,
            int scoreJ1,
            long tempsJ0Ms,
            long tempsJ1Ms,
            boolean horlogeEpuiseeJ0,
            boolean horlogeEpuiseeJ1
    ) {
        int winner() {
            if (scoreJ0 > scoreJ1) return 0;
            if (scoreJ1 > scoreJ0) return 1;
            return -1;
        }
    }

    /**
     * Fait jouer une partie en gérant le chrono.
     * Si une IA dépasse son temps total, elle ne peut plus jouer.
     */
    public static MatchResult jouerAvecHorloge(ActionStrategy player0, ActionStrategy player1,
                                               int rows, int cols) {
        Board board = new Board(rows, cols);

        // Budget total par joueur : nb_coups_initiaux × 1 seconde (règle du tournoi)
        int nbCoupsInitiaux = board.getAvailableActions().size();
        long budgetParJoueurMs = (long) nbCoupsInitiaux * 1000;

        long[] tempsRestantMs = { budgetParJoueurMs, budgetParJoueurMs };
        long[] tempsConsommeMs = { 0L, 0L };
        boolean[] horlogeEpuisee = { false, false };

        ActionStrategy[] strategies = { player0, player1 };
        int joueurCourant = 0;

        while (!board.isFinished()) {

            // Si les deux horloges sont épuisées, on arrête
            if (horlogeEpuisee[0] && horlogeEpuisee[1]) break;

            // Si l'horloge du joueur courant est épuisée, on passe son tour
            if (horlogeEpuisee[joueurCourant]) {
                joueurCourant = 1 - joueurCourant;
                continue;
            }

            // Mesure du temps de calcul
            long debut = System.currentTimeMillis();
            silence();
            Action action = strategies[joueurCourant].selectAction(board, joueurCourant);
            parole();
            long duree = System.currentTimeMillis() - debut;

            // Mise à jour de l'horloge
            tempsConsommeMs[joueurCourant] += duree;
            tempsRestantMs[joueurCourant]  -= duree;

            if (tempsRestantMs[joueurCourant] <= 0) {
                horlogeEpuisee[joueurCourant] = true;
                tempsRestantMs[joueurCourant] = 0;
            }

            // Coup invalide ou null -> on passe le tour
            if (action == null || !board.isValid(action)) {
                joueurCourant = 1 - joueurCourant;
                continue;
            }

            int nbFerme = board.apply(action, joueurCourant);

            // Si aucune case fermée, c'est au tour de l'adversaire
            if (nbFerme == 0) joueurCourant = 1 - joueurCourant;
        }

        return new MatchResult(
                board.getScore(0),
                board.getScore(1),
                tempsConsommeMs[0],
                tempsConsommeMs[1],
                horlogeEpuisee[0],
                horlogeEpuisee[1]
        );
    }

    // Lance un match aller et un match retour
    private static int[] jouerDuelAllerRetour(String nom0, ActionStrategy p0,
                                              String nom1, ActionStrategy p1,
                                              int rows, int cols) {
        int ptsP0 = 0, ptsP1 = 0;

        System.out.printf("%n  Grille %dx%d%n", rows, cols);

        // Match aller, p0 commence
        System.out.printf("    Aller  (%s commence) ... ", nom0);
        MatchResult aller = jouerAvecHorloge(p0, p1, rows, cols);
        afficherResultatMatch(aller, nom0, nom1);

        if (aller.winner() == 0)       { ptsP0 += 3; }
        else if (aller.winner() == 1)  { ptsP1 += 3; }
        else                           { ptsP0 += 1; ptsP1 += 1; }

        // Match retour, p1 commence
        System.out.printf("    Retour (%s commence) ... ", nom1);
        MatchResult retour = jouerAvecHorloge(p1, p0, rows, cols);
        MatchResult retourInverse = new MatchResult(
                retour.scoreJ1(), retour.scoreJ0(),
                retour.tempsJ1Ms(), retour.tempsJ0Ms(),
                retour.horlogeEpuiseeJ1(), retour.horlogeEpuiseeJ0()
        );
        afficherResultatMatch(retourInverse, nom0, nom1);

        if (retourInverse.winner() == 0)       { ptsP0 += 3; }
        else if (retourInverse.winner() == 1)  { ptsP1 += 3; }
        else                                   { ptsP0 += 1; ptsP1 += 1; }

        System.out.printf("    → Points sur cette grille : %s=%d | %s=%d%n",
                nom0, ptsP0, nom1, ptsP1);

        return new int[]{ ptsP0, ptsP1 };
    }


    private static void afficherResultatMatch(MatchResult r, String nom0, String nom1) {
        String vainqueur = (r.winner() == 0) ? nom0 : (r.winner() == 1) ? nom1 : "NUL";
        System.out.printf("Score %d-%d → %s", r.scoreJ0(), r.scoreJ1(), vainqueur);

        if (r.horlogeEpuiseeJ0()) System.out.printf(" [Temps %s épuisé]", nom0);
        if (r.horlogeEpuiseeJ1()) System.out.printf(" [Temps %s épuisé]", nom1);

        System.out.printf("  (temps: %s=%.1fs | %s=%.1fs)%n",
                nom0, r.tempsJ0Ms() / 1000.0,
                nom1, r.tempsJ1Ms() / 1000.0);
    }

    // Enchaîne les duels sur toutes les grilles prévues
    private static void lancerTournoi(String nom0, ActionStrategy p0,
                                      String nom1, ActionStrategy p1,
                                      int[][] grilles) {
        System.out.println("\n" + "=".repeat(60));
        System.out.printf("  TOURNOI : %s  VS  %s%n", nom0, nom1);
        System.out.println("=".repeat(60));

        int totalPtsP0 = 0, totalPtsP1 = 0;
        int victoiresP0 = 0, victoiresP1 = 0, nuls = 0;

        for (int[] grille : grilles) {
            int[] pts = jouerDuelAllerRetour(nom0, p0, nom1, p1, grille[0], grille[1]);
            totalPtsP0 += pts[0];
            totalPtsP1 += pts[1];

            // Comptage victoires/nuls par grille (sur les 2 matchs aller/retour)
            if (pts[0] > pts[1])      victoiresP0++;
            else if (pts[1] > pts[0]) victoiresP1++;
            else                      nuls++;
        }

        // Classement final
        System.out.println("\n" + "=".repeat(60));
        System.out.println("  CLASSEMENT FINAL");
        System.out.println("=".repeat(60));
        System.out.printf("  %-30s : %d pts (%d grilles gagnées)%n",
                nom0, totalPtsP0, victoiresP0);
        System.out.printf("  %-30s : %d pts (%d grilles gagnées)%n",
                nom1, totalPtsP1, victoiresP1);
        System.out.printf("  Grilles nulles : %d%n", nuls);

        String vainqueur = (totalPtsP0 > totalPtsP1) ? nom0
                : (totalPtsP1 > totalPtsP0) ? nom1
                  : "ÉGALITÉ";
        System.out.println("\n  → Vainqueur du tournoi : " + vainqueur);
        System.out.println("=".repeat(60));
    }

    // Configuration du match
    public static void main(String[] args) {

        // Stratégie A
        String nomA = "Alpha-Beta TT";
        ActionStrategy strA = new AlphaBetaTTActionStrategy(
                new AlphaBetaPruningObserver(),
                new NodeCounterObserver()
        );

        // Stratégie B
        String nomB = "MCTS Expert";
        ActionStrategy strB = new ExpertActionStrategy(new AlphaBetaPruningObserver(),
                new NodeCounterObserver());

        // Grilles à tester (reproduit les conditions du tournoi)
        int[][] grilles = {
                {3, 3},
                {4, 4},
                {4, 5},
                {5, 5},
                {6, 6},
                {6, 8}
        };

        lancerTournoi(nomA, strA, nomB, strB, grilles);

        // Nécessaire pour terminer les threads des stratégies MCTS
        System.exit(0);
    }
}