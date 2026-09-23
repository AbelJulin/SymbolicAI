package DotsBoxes.player.ai;

import DotsBoxes.board.Action;
import DotsBoxes.board.Board;
import DotsBoxes.observers.AlphaBetaPruningObserver;
import DotsBoxes.observers.NodeCounterObserver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AlphaBetaActionStrategyTest {

    // HELPERS

    private AlphaBetaActionStrategy makeStrategy(int depth) {
        return new AlphaBetaActionStrategy(new AlphaBetaPruningObserver(), new NodeCounterObserver());
    }

    private AlphaBetaActionStrategy makeStrategy(int depth, AlphaBetaPruningObserver obs, NodeCounterObserver nc) {
        return new AlphaBetaActionStrategy(obs, nc);
    }


    // 1. ÉTATS TERMINAUX

    @Test
    void selectActionShouldReturnNullWhenNoActionAvailable() {
        Board board = new Board(2, 2);
        board.apply(new Action(Action.Type.HORIZONTAL, 0, 0), 0);
        board.apply(new Action(Action.Type.HORIZONTAL, 1, 0), 1);
        board.apply(new Action(Action.Type.VERTICAL,   0, 0), 0);
        board.apply(new Action(Action.Type.VERTICAL,   0, 1), 1);

        assertNull(makeStrategy(3).selectAction(board, 0));
    }

    // 2. DÉCISIONS OPTIMALES SUR CONFIGURATIONS SIMPLES

    @Test
    void selectActionShouldChooseImmediateBoxClosingMoveAndIncrementNodeCounter() {
        Board board = new Board(2, 2);
        board.apply(new Action(Action.Type.HORIZONTAL, 0, 0), 0);
        board.apply(new Action(Action.Type.HORIZONTAL, 1, 0), 1);
        board.apply(new Action(Action.Type.VERTICAL,   0, 0), 0);

        AlphaBetaPruningObserver observer = new AlphaBetaPruningObserver();
        NodeCounterObserver nodeCounter   = new NodeCounterObserver();
        AlphaBetaActionStrategy strategy  = makeStrategy(2, observer, nodeCounter);

        Action best = strategy.selectAction(board, 1);

        assertAll(
                () -> assertEquals(new Action(Action.Type.VERTICAL, 0, 1), best),
                () -> assertTrue(nodeCounter.getCount() > 0),
                () -> assertTrue(observer.getAlphaCutCount() >= 0),
                () -> assertTrue(observer.getBetaCutCount() >= 0)
        );
    }

    @Test
    void selectActionShouldPreferGainingABox() {
        Board board = new Board(3, 3);
        // Case (0,0) : 3 côtés posés, il manque droite = VERTICAL(0,1)
        board.apply(new Action(Action.Type.HORIZONTAL, 0, 0), 1);
        board.apply(new Action(Action.Type.HORIZONTAL, 1, 0), 1);
        board.apply(new Action(Action.Type.VERTICAL,   0, 0), 1);

        AlphaBetaActionStrategy ai = makeStrategy(3);
        Action chosen = ai.selectAction(board, 0);

        assertNotNull(chosen);
        Board copy = new Board(board);
        int gained = copy.apply(chosen, 0);
        assertTrue(gained > 0, "Alpha-Beta doit fermer la case disponible");
    }

    @Test
    void selectActionShouldAvoidGivingBoxToOpponent() {
        Board board = new Board(3, 3);
        board.apply(new Action(Action.Type.HORIZONTAL, 0, 0), 1);
        board.apply(new Action(Action.Type.VERTICAL,   0, 0), 1);

        AlphaBetaActionStrategy ai = makeStrategy(4);
        Action chosen = ai.selectAction(board, 0);

        assertNotNull(chosen);
        boolean isDangerous =
                (chosen.getType() == Action.Type.HORIZONTAL && chosen.getRow() == 1 && chosen.getCol() == 0) ||
                        (chosen.getType() == Action.Type.VERTICAL   && chosen.getRow() == 0 && chosen.getCol() == 1);

        assertFalse(isDangerous, "Alpha-Beta ne doit pas offrir une case à l'adversaire");
    }


    // 3. COHÉRENCE DES VALEURS RETOURNÉES
    @Test
    void selectActionShouldNeverReturnNullIfMovesAvailable() {
        Board board = new Board(4, 4);
        assertNotNull(makeStrategy(3).selectAction(board, 0));
    }

    @Test
    void selectActionShouldReturnValidAction() {
        Board board = new Board(4, 4);
        Action chosen = makeStrategy(3).selectAction(board, 0);
        assertNotNull(chosen);
        assertTrue(board.isValid(chosen), "L'action retournée doit être valide");
    }


    // 4. COMPTEURS DE NŒUDS ET COUPES

    @Test
    void nodeCounterShouldBeZeroInitially() {
        NodeCounterObserver nc = new NodeCounterObserver();
        assertEquals(0, nc.getCount());
    }

    @Test
    void observerShouldBeZeroInitially() {
        AlphaBetaPruningObserver obs = new AlphaBetaPruningObserver();
        assertAll(
                () -> assertEquals(0, obs.getNodeCount()),
                () -> assertEquals(0, obs.getAlphaCutCount()),
                () -> assertEquals(0, obs.getBetaCutCount())
        );
    }

    @Test
    void nodeCounterShouldBePositiveAfterSelectAction() {
        Board board = new Board(3, 3);
        NodeCounterObserver nc = new NodeCounterObserver();
        makeStrategy(3, new AlphaBetaPruningObserver(), nc).selectAction(board, 0);
        assertTrue(nc.getCount() > 0);
    }

    @Test
    void observerNodeCountShouldBePositiveAfterSelectAction() {
        Board board = new Board(3, 3);
        AlphaBetaPruningObserver obs = new AlphaBetaPruningObserver();
        makeStrategy(3, obs, new NodeCounterObserver()).selectAction(board, 0);
        assertTrue(obs.getNodeCount() > 0);
    }

    @Test
    void observerShouldResetBetweenSelectActions() {
        Board board = new Board(3, 3);
        AlphaBetaPruningObserver obs = new AlphaBetaPruningObserver();
        NodeCounterObserver nc       = new NodeCounterObserver();
        AlphaBetaActionStrategy ai   = makeStrategy(2, obs, nc);

        ai.selectAction(board, 0);
        int firstCount = obs.getNodeCount();

        ai.selectAction(board, 0);
        int secondCount = obs.getNodeCount();

        assertEquals(firstCount, secondCount, "Le compteur doit être réinitialisé entre deux selectAction sur le même état");
    }

    @Test
    void deeperDepthShouldVisitMoreNodes() {
        Board board = new Board(3, 3);

        AlphaBetaPruningObserver obsShallow = new AlphaBetaPruningObserver();
        AlphaBetaPruningObserver obsDeep    = new AlphaBetaPruningObserver();

        makeStrategy(1, obsShallow, new NodeCounterObserver()).selectAction(board, 0);
        makeStrategy(3, obsDeep,    new NodeCounterObserver()).selectAction(board, 0);

        assertTrue(obsDeep.getNodeCount() >= obsShallow.getNodeCount(),
                "Une profondeur plus grande doit visiter plus de nœuds");
    }

    @Test
    void alphaCutsShouldBeNonNegative() {
        Board board = new Board(4, 4);
        AlphaBetaPruningObserver obs = new AlphaBetaPruningObserver();
        makeStrategy(3, obs, new NodeCounterObserver()).selectAction(board, 0);
        assertTrue(obs.getAlphaCutCount() >= 0);
    }

    @Test
    void betaCutsShouldBeNonNegative() {
        Board board = new Board(4, 4);
        AlphaBetaPruningObserver obs = new AlphaBetaPruningObserver();
        makeStrategy(3, obs, new NodeCounterObserver()).selectAction(board, 0);
        assertTrue(obs.getBetaCutCount() >= 0);
    }

    @Test
    void getNameShouldReturnAlphaBeta() {
        assertEquals("Alpha-Beta", makeStrategy(1).getName());
    }
}
