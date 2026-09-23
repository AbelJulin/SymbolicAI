package DotsBoxes.player.ai;

import DotsBoxes.board.Action;
import DotsBoxes.board.Board;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MinimaxActionStrategyTest {

    @Test
    void selectActionShouldReturnNullWhenNoActionAvailable() {
        Board board = new Board(2, 2);
        board.apply(new Action(Action.Type.HORIZONTAL, 0, 0), 0);
        board.apply(new Action(Action.Type.HORIZONTAL, 1, 0), 1);
        board.apply(new Action(Action.Type.VERTICAL, 0, 0), 0);
        board.apply(new Action(Action.Type.VERTICAL, 0, 1), 1);

        MinimaxActionStrategy strategy = new MinimaxActionStrategy(3);

        assertNull(strategy.selectAction(board, 0));
    }

    @Test
    void selectActionShouldChooseImmediateBoxClosingMove() {
        Board board = new Board(2, 2);
        board.apply(new Action(Action.Type.HORIZONTAL, 0, 0), 0);
        board.apply(new Action(Action.Type.HORIZONTAL, 1, 0), 1);
        board.apply(new Action(Action.Type.VERTICAL, 0, 0), 0);

        MinimaxActionStrategy strategy = new MinimaxActionStrategy(2);

        Action best = strategy.selectAction(board, 1);

        assertEquals(new Action(Action.Type.VERTICAL, 0, 1), best);
    }

    // 1. ÉTATS TERMINAUX

    /**
     Sur un plateau entièrement rempli, isFinished() doit être vrai
     et le score retourné doit refléter l'état final.
     */
    @Test
    public void testEtatTerminal_plateauPlein() {
        // Plateau 2x2 : 1 seule case possible
        Board board = new Board(2, 2);

        // On ferme la seule case (4 segments)
        board.apply(new Action(Action.Type.HORIZONTAL, 0, 0), 0);
        board.apply(new Action(Action.Type.HORIZONTAL, 1, 0), 0);
        board.apply(new Action(Action.Type.VERTICAL,   0, 0), 0);
        board.apply(new Action(Action.Type.VERTICAL,   0, 1), 0);

        assertTrue(board.isFinished(), "Le plateau doit être terminé");
        assertEquals(1, board.getScore(0), "Joueur 0 doit avoir 1 case");
        assertEquals(0, board.getScore(1), "Joueur 1 doit avoir 0 case");
    }

    /**
     minimax sur un état terminal doit retourner score(playerId) - score(adversaire) sans explorer de coup.
     */
    @Test
    public void testMinimax_etatTerminal_retourneScoreRelatif() {
        Board board = new Board(2, 2);
        board.apply(new Action(Action.Type.HORIZONTAL, 0, 0), 0);
        board.apply(new Action(Action.Type.HORIZONTAL, 1, 0), 0);
        board.apply(new Action(Action.Type.VERTICAL,   0, 0), 0);
        board.apply(new Action(Action.Type.VERTICAL,   0, 1), 0);

        MinimaxActionStrategy ai = new MinimaxActionStrategy(3);

        // score initial = getScore(0) - getScore(1) = 1 - 0 = 1
        assertEquals(1,  ai.minimax(board, 3, 0, 0, 1));

        // depuis le point de vue de joueur 1 : score initial = 0 - 1 = -1
        assertEquals(-1, ai.minimax(board, 3, 1, 1, -1));
    }

    /**
     minimax à profondeur 0 doit se comporter comme un état terminal.
     */
    @Test
    public void testMinimax_profondeurZero_retourneEvaluation() {
        Board board = new Board(3, 3);
        MinimaxActionStrategy ai = new MinimaxActionStrategy(0);

        // Plateau vide, score initial = 0
        assertEquals(0, ai.minimax(board, 0, 0, 0, 0));
    }

    // 2. DÉCISIONS OPTIMALES SUR CONFIGURATIONS SIMPLES

    /**
     Si une case peut être fermée immédiatement, l'IA doit la saisir.
     */
    @Test
    public void testSelectAction_fermetureDirece() {
        Board board = new Board(2, 2);
        board.apply(new Action(Action.Type.HORIZONTAL, 0, 0), 1);
        board.apply(new Action(Action.Type.HORIZONTAL, 1, 0), 1);
        board.apply(new Action(Action.Type.VERTICAL,   0, 0), 1);

        MinimaxActionStrategy ai = new MinimaxActionStrategy(3);
        Action chosen = ai.selectAction(board, 0);

        assertNotNull(chosen);
        assertEquals(Action.Type.VERTICAL, chosen.getType());
        assertEquals(0, chosen.getRow());
        assertEquals(1, chosen.getCol());
    }

    /**
     L'IA (joueur 0) doit préférer un coup qui lui rapporte une case plutôt qu'un coup neutre.
     */
    @Test
    public void testSelectAction_prefereGagnerUneCase() {
        Board board = new Board(3, 3);

        board.apply(new Action(Action.Type.HORIZONTAL, 0, 0), 1);
        board.apply(new Action(Action.Type.HORIZONTAL, 1, 0), 1);
        board.apply(new Action(Action.Type.VERTICAL,   0, 0), 1);

        MinimaxActionStrategy ai = new MinimaxActionStrategy(3);
        Action chosen = ai.selectAction(board, 0);

        assertNotNull(chosen);
        Board boardCopy = new Board(board);
        int gained = boardCopy.apply(chosen, 0);
        assertTrue(gained > 0, "L'IA doit choisir un coup qui ferme au moins une case");
    }

    /**
     L'IA ne doit pas offrir une case à l'adversaire :
     si jouer un coup donne la 3ème arête d'une case, l'IA doit l'éviter si un autre coup existe.
     */
    @Test
    public void testSelectAction_eviteOffrir_uneCase() {
        Board board = new Board(3, 3);
        board.apply(new Action(Action.Type.HORIZONTAL, 0, 0), 1);
        board.apply(new Action(Action.Type.VERTICAL,   0, 0), 1);

        MinimaxActionStrategy ai = new MinimaxActionStrategy(4);
        Action chosen = ai.selectAction(board, 0);

        assertNotNull(chosen);
        boolean isDangerous =
                (chosen.getType() == Action.Type.HORIZONTAL && chosen.getRow() == 1 && chosen.getCol() == 0) ||
                        (chosen.getType() == Action.Type.VERTICAL   && chosen.getRow() == 0 && chosen.getCol() == 1);

        assertFalse(isDangerous, "L'IA ne doit pas offrir une case à l'adversaire");
    }

    // 3. COHÉRENCE DES VALEURS RETOURNÉES

    /**
     Le score retourné par minimax doit être borné : entre -(nb_cases) et +(nb_cases).
     */
    @Test
    public void testMinimax_valeurBornee() {
        Board board = new Board(3, 3); // 4 cases possibles
        MinimaxActionStrategy ai = new MinimaxActionStrategy(3);
        int maxCases = (3 - 1) * (3 - 1); // 4

        // score initial = 0 sur plateau vide
        int score = ai.minimax(board, 3, 0, 0, 0);

        assertTrue(score >= -maxCases && score <= maxCases,
                "Le score doit être entre -" + maxCases + " et " + maxCases + ", obtenu : " + score);
    }

    /**
     Le score de minimax vu par joueur 0 doit être l'opposé de celui vu par joueur 1 (symétrie du jeu).
     */
    @Test
    public void testMinimax_symetrie_joueurs() {
        Board board = new Board(3, 3);
        MinimaxActionStrategy ai = new MinimaxActionStrategy(3);

        // score initial = 0 sur plateau vide, currentPlayer = 0 dans les deux cas
        int scoreJ0 = ai.minimax(board, 3, 0, 0, 0);
        int scoreJ1 = ai.minimax(board, 3, 0, 1, 0);

        assertEquals(-scoreJ0, scoreJ1,
                "Le score de J0 doit être l'opposé de celui de J1");
    }

    /**
     selectAction ne doit jamais retourner null si des coups sont disponibles.
     */
    @Test
    public void testSelectAction_jamaisNull_siCoupsDisponibles() {
        Board board = new Board(4, 4);
        MinimaxActionStrategy ai = new MinimaxActionStrategy(3);

        Action chosen = ai.selectAction(board, 0);
        assertNotNull(chosen, "selectAction ne doit pas retourner null sur un plateau non vide");
    }

    /**
     selectAction doit retourner null si le plateau est terminé.
     */
    @Test
    public void testSelectAction_retourneNull_siPlateauTermine() {
        Board board = new Board(2, 2);
        board.apply(new Action(Action.Type.HORIZONTAL, 0, 0), 0);
        board.apply(new Action(Action.Type.HORIZONTAL, 1, 0), 0);
        board.apply(new Action(Action.Type.VERTICAL,   0, 0), 0);
        board.apply(new Action(Action.Type.VERTICAL,   0, 1), 0);

        MinimaxActionStrategy ai = new MinimaxActionStrategy(3);
        assertNull(ai.selectAction(board, 0), "selectAction doit retourner null si le plateau est fini");
    }

    /**
     Un coup retourné par selectAction doit toujours être valide sur le plateau courant.
     */
    @Test
    public void testSelectAction_retourneActionValide() {
        Board board = new Board(4, 4);
        MinimaxActionStrategy ai = new MinimaxActionStrategy(3);

        Action chosen = ai.selectAction(board, 0);
        assertNotNull(chosen);
        assertTrue(board.isValid(chosen), "L'action retournée doit être valide sur le plateau");
    }


    // 4. COMPTEUR DE NŒUDS

    /**
     Le compteur doit être à 0 avant toute exploration.
     */
    @Test
    public void testObserver_initialementAZero() {
        MinimaxActionStrategy ai = new MinimaxActionStrategy(3);
        assertEquals(0, ai.getObserver().getCount(), "Le compteur doit être à 0 avant toute exploration");
    }

    /**
     Après un selectAction, le compteur doit être strictement positif.
     */
    @Test
    public void testObserver_positifApresSelectAction() {
        Board board = new Board(3, 3);
        MinimaxActionStrategy ai = new MinimaxActionStrategy(3);

        ai.selectAction(board, 0);

        assertTrue(ai.getObserver().getCount() > 0, "Le compteur doit être > 0 après une exploration");
    }

    /**
     reset() doit remettre le compteur à 0.
     */
    @Test
    public void testObserver_resetRemetAZero() {
        Board board = new Board(3, 3);
        MinimaxActionStrategy ai = new MinimaxActionStrategy(3);

        ai.selectAction(board, 0);
        assertTrue(ai.getObserver().getCount() > 0);

        ai.getObserver().reset();
        assertEquals(0, ai.getObserver().getCount(), "Le compteur doit être à 0 après reset()");
    }

    /**
     Une profondeur plus grande doit visiter strictement plus de nœuds.
     */
    @Test
    public void testObserver_plusDeNoeudsPourPlusGrandeProfondeur() {
        Board board = new Board(3, 3);

        MinimaxActionStrategy aiShallow = new MinimaxActionStrategy(1);
        MinimaxActionStrategy aiDeep    = new MinimaxActionStrategy(3);

        aiShallow.selectAction(board, 0);
        int nodesShallow = aiShallow.getObserver().getCount();

        aiDeep.selectAction(board, 0);
        int nodesDeep = aiDeep.getObserver().getCount();

        assertTrue(nodesDeep > nodesShallow,
                "Une profondeur plus grande doit visiter plus de nœuds : " + nodesDeep + " > " + nodesShallow);
    }

    /**
     Un plateau plus grand doit visiter strictement plus de noeuds à profondeur égale.
     */
    @Test
    public void testObserver_plusDeNoeudsPourPlusGrandPlateau() {
        MinimaxActionStrategy aiSmall = new MinimaxActionStrategy(3);
        MinimaxActionStrategy aiBig   = new MinimaxActionStrategy(3);

        aiSmall.selectAction(new Board(3, 3), 0);
        int nodesSmall = aiSmall.getObserver().getCount();

        aiBig.selectAction(new Board(4, 4), 0);
        int nodesBig = aiBig.getObserver().getCount();

        assertTrue(nodesBig > nodesSmall,
                "Un plateau plus grand doit visiter plus de nœuds : " + nodesBig + " > " + nodesSmall);
    }

    /**
     Sur un plateau terminal, aucun noeud interne ne doit être visité :
     le compteur doit rester à 1 (juste la feuille racine).
     */
    @Test
    public void testObserver_etatTerminal_unSeulNoeud() {
        Board board = new Board(2, 2);
        board.apply(new Action(Action.Type.HORIZONTAL, 0, 0), 0);
        board.apply(new Action(Action.Type.HORIZONTAL, 1, 0), 0);
        board.apply(new Action(Action.Type.VERTICAL,   0, 0), 0);
        board.apply(new Action(Action.Type.VERTICAL,   0, 1), 0);

        MinimaxActionStrategy ai = new MinimaxActionStrategy(3);
        ai.minimax(board, 3, 0, 0, 1);

        assertEquals(1, ai.getObserver().getCount(),
                "Sur un état terminal, un seul nœud doit être visité");
    }

    /**
     Le compteur repart bien de zéro entre deux appels à selectAction.
     Le second appel ne doit pas cumuler avec le premier.
     */
    @Test
    public void testObserver_resetEntreDeuxSelectAction() {
        Board board = new Board(3, 3);
        MinimaxActionStrategy ai = new MinimaxActionStrategy(2);

        ai.selectAction(board, 0);
        int firstCount = ai.getObserver().getCount();

        ai.selectAction(board, 0);
        int secondCount = ai.getObserver().getCount();

        // Les deux explorations partent du même état → même nombre de nœuds
        assertEquals(firstCount, secondCount,
                "Le compteur doit être réinitialisé entre deux selectAction sur le même état");
    }


    @Test
    void getNameShouldReturnMinimax() {
        assertEquals("Minimax", new MinimaxActionStrategy(1).getName());
    }
}
