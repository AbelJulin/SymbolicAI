package DotsBoxes.player.ai;

import DotsBoxes.board.Action;
import DotsBoxes.board.Board;
import DotsBoxes.observers.AlphaBetaPruningObserver;
import DotsBoxes.observers.NodeCounterObserver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests pour vérifier que l'IA Alpha-Beta avec Table de Transposition fonctionne.
 */
class AlphaBetaTTActionStrategyTest {

    private AlphaBetaTTActionStrategy makeStrategy() {
        return new AlphaBetaTTActionStrategy(
                new AlphaBetaPruningObserver(),
                new NodeCounterObserver()
        );
    }

    private AlphaBetaTTActionStrategy makeStrategy(AlphaBetaPruningObserver obs,
                                                   NodeCounterObserver nc) {
        return new AlphaBetaTTActionStrategy(obs, nc);
    }

    // Tests sur la fin de partie
    @Test
    void selectActionShouldReturnNullWhenBoardIsFinished() {
        Board board = new Board(2, 2);
        board.apply(new Action(Action.Type.HORIZONTAL, 0, 0), 0);
        board.apply(new Action(Action.Type.HORIZONTAL, 1, 0), 1);
        board.apply(new Action(Action.Type.VERTICAL,   0, 0), 0);
        board.apply(new Action(Action.Type.VERTICAL,   0, 1), 1);

        assertNull(makeStrategy().selectAction(board, 0));
    }

    @Test
    void selectActionShouldReturnOnlyAvailableMove() {
        Board board = new Board(2, 2);
        board.apply(new Action(Action.Type.HORIZONTAL, 0, 0), 0);
        board.apply(new Action(Action.Type.HORIZONTAL, 1, 0), 1);
        board.apply(new Action(Action.Type.VERTICAL,   0, 0), 0);

        // Un seul coup disponible : VERTICAL(0,1)
        Action chosen = makeStrategy().selectAction(board, 1);

        assertEquals(new Action(Action.Type.VERTICAL, 0, 1), chosen);
    }

    @Test
    void alphaBetaTTAtDepthZeroShouldReturnHeuristicValue() {
        Board board = new Board(3, 3);
        AlphaBetaTTActionStrategy ai = makeStrategy();

        // Plateau vide, profondeur 0 → évaluation heuristique (pas de score brut)
        int val = ai.alphaBetaTT(board, 0, 0, 0, 0,
                Integer.MIN_VALUE, Integer.MAX_VALUE,
                System.currentTimeMillis() + 5000);

        assertEquals(HeuristicFunction.evaluate(board, 0), val);
    }

    // Tests sur les choix de l'IA
    @Test
    void selectActionShouldChooseImmediateBoxClosingMove() {
        Board board = new Board(2, 2);
        board.apply(new Action(Action.Type.HORIZONTAL, 0, 0), 0);
        board.apply(new Action(Action.Type.HORIZONTAL, 1, 0), 1);
        board.apply(new Action(Action.Type.VERTICAL,   0, 0), 0);

        AlphaBetaPruningObserver obs = new AlphaBetaPruningObserver();
        NodeCounterObserver nc       = new NodeCounterObserver();
        AlphaBetaTTActionStrategy ai = makeStrategy(obs, nc);

        Action best = ai.selectAction(board, 1);

        assertAll(
                () -> assertEquals(new Action(Action.Type.VERTICAL, 0, 1), best),
                () -> assertTrue(nc.getCount() > 0),
                () -> assertTrue(obs.getAlphaCutCount() >= 0),
                () -> assertTrue(obs.getBetaCutCount() >= 0)
        );
    }

    @Test
    void selectActionShouldPreferGainingABox() {
        Board board = new Board(3, 3);
        // Case (0,0) : 3 côtés posés, il manque VERTICAL(0,1)
        board.apply(new Action(Action.Type.HORIZONTAL, 0, 0), 1);
        board.apply(new Action(Action.Type.HORIZONTAL, 1, 0), 1);
        board.apply(new Action(Action.Type.VERTICAL,   0, 0), 1);

        Action chosen = makeStrategy().selectAction(board, 0);

        assertNotNull(chosen);
        Board copy = new Board(board);
        int gained = copy.apply(chosen, 0);
        assertTrue(gained > 0, "AlphaBetaTT doit fermer la case disponible");
    }

    @Test
    void selectActionShouldAvoidGivingBoxToOpponent() {
        Board board = new Board(3, 3);
        board.apply(new Action(Action.Type.HORIZONTAL, 0, 0), 1);
        board.apply(new Action(Action.Type.VERTICAL,   0, 0), 1);

        Action chosen = makeStrategy().selectAction(board, 0);

        assertNotNull(chosen);
        boolean isDangerous =
                (chosen.getType() == Action.Type.HORIZONTAL && chosen.getRow() == 1 && chosen.getCol() == 0) ||
                        (chosen.getType() == Action.Type.VERTICAL   && chosen.getRow() == 0 && chosen.getCol() == 1);

        assertFalse(isDangerous, "AlphaBetaTT ne doit pas offrir une case à l'adversaire");
    }


    // Tests sur la table de transposition
    @Test
    void transpositionTableShouldBeNonEmptyAfterSelectAction() {
        Board board = new Board(3, 3);
        AlphaBetaTTActionStrategy ai = makeStrategy();

        ai.selectAction(board, 0);

        assertTrue(ai.getTableTransposition().getSize() > 0,
                "La table de transposition doit contenir des entrées après la recherche");
    }

    @Test
    void transpositionTableShouldRegisterHitsDuringIterativeDeepening() {
        Board board = new Board(3, 3);
        AlphaBetaTTActionStrategy ai = makeStrategy();

        ai.selectAction(board, 0);
        int hits = ai.getTableTransposition().getHits();
        System.out.printf("[TT hits 3x3] hits=%d | consultations=%d%n",
                hits, ai.getTableTransposition().getConsultations());

        assertTrue(hits > 0,
                "Des hits doivent être enregistrés grâce à l'iterative deepening");
    }

    @Test
    void transpositionTableShouldAccumulateAcrossSelectActionCalls() {
        Board board = new Board(3, 3);
        AlphaBetaTTActionStrategy ai = makeStrategy();

        ai.selectAction(board, 0);
        int sizeApremierAppel = ai.getTableTransposition().getSize();
        Action action = board.getAvailableActions().get(0);
        board.apply(action, 0);

        ai.selectAction(board, 1);
        int sizeApresDeuxiemeAppel = ai.getTableTransposition().getSize();

        // La table doit avoir au moins autant d'entrées
        assertTrue(sizeApresDeuxiemeAppel >= sizeApremierAppel,
                "La table doit conserver ses entrées entre deux coups de la même partie");
    }

    // Tests sur le Hash Zorbist
    @Test
    void zobristShouldReturnSameHashForSameBoard() {
        Board board = new Board(3, 3);
        board.apply(new Action(Action.Type.HORIZONTAL, 0, 0), 0);
        board.apply(new Action(Action.Type.VERTICAL,   0, 0), 1);

        ZobristHash zobrist = new ZobristHash(board.getRows(), board.getCols());

        long hash1 = zobrist.compute(board);
        long hash2 = zobrist.compute(board);

        assertEquals(hash1, hash2,
                "Le même plateau doit toujours produire le même hash");
    }

    @Test
    void zobristShouldReturnDifferentHashForDifferentBoards() {
        Board board1 = new Board(3, 3);
        Board board2 = new Board(3, 3);

        board1.apply(new Action(Action.Type.HORIZONTAL, 0, 0), 0);
        board2.apply(new Action(Action.Type.VERTICAL,   0, 0), 0);

        ZobristHash zobrist = new ZobristHash(3, 3);

        long hash1 = zobrist.compute(board1);
        long hash2 = zobrist.compute(board2);

        assertNotEquals(hash1, hash2,
                "Deux plateaux différents doivent (quasi-)toujours produire des hashs différents");
    }

    @Test
    void zobristWithPlayerShouldDifferFromWithoutPlayer() {
        Board board = new Board(3, 3);
        ZobristHash zobrist = new ZobristHash(3, 3);

        long hashSansJoueur  = zobrist.compute(board);
        long hashAvecJoueur0 = zobrist.computeWithPlayer(board, 0);
        long hashAvecJoueur1 = zobrist.computeWithPlayer(board, 1);

        assertAll(
                () -> assertNotEquals(hashSansJoueur, hashAvecJoueur0),
                () -> assertNotEquals(hashSansJoueur, hashAvecJoueur1),
                () -> assertNotEquals(hashAvecJoueur0, hashAvecJoueur1,
                        "J0 et J1 doivent produire des hashs distincts sur le même plateau")
        );
    }

    // Tests sur la validité et robustesse
    @Test
    void selectActionShouldNeverReturnNullIfMovesAvailable() {
        Board board = new Board(4, 4);
        assertNotNull(makeStrategy().selectAction(board, 0));
    }

    @Test
    void selectActionShouldReturnValidAction() {
        Board board = new Board(4, 4);
        Action chosen = makeStrategy().selectAction(board, 0);
        assertNotNull(chosen);
        assertTrue(board.isValid(chosen), "L'action retournée doit être valide sur le plateau");
    }

    @Test
    void getNameShouldReturnExpectedName() {
        assertEquals("Alpha-Beta + Table de Transposition", makeStrategy().getName());
    }

    @Test
    void nodeCounterShouldBePositiveAfterSelectAction() {
        Board board = new Board(3, 3);
        NodeCounterObserver nc       = new NodeCounterObserver();
        AlphaBetaTTActionStrategy ai = makeStrategy(new AlphaBetaPruningObserver(), nc);

        ai.selectAction(board, 0);

        assertTrue(nc.getCount() > 0,
                "Le compteur de nœuds doit être positif après une recherche");
    }
}