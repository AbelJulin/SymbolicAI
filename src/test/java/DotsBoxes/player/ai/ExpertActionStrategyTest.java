package DotsBoxes.player.ai;

import DotsBoxes.board.Action;
import DotsBoxes.board.Board;
import DotsBoxes.observers.AlphaBetaPruningObserver;
import DotsBoxes.observers.NodeCounterObserver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExpertActionStrategyTest {

    @Test
    void selectActionShouldReturnNullWhenBoardIsFinished() {
        Board board = new Board(2, 2);
        board.apply(new Action(Action.Type.HORIZONTAL, 0, 0), 0);
        board.apply(new Action(Action.Type.HORIZONTAL, 1, 0), 1);
        board.apply(new Action(Action.Type.VERTICAL, 0, 0), 0);
        board.apply(new Action(Action.Type.VERTICAL, 0, 1), 1);

        ExpertActionStrategy strategy = new ExpertActionStrategy(new AlphaBetaPruningObserver(),
                new NodeCounterObserver());

        assertNull(strategy.selectAction(board, 0));
    }

    @Test
    void selectActionShouldReturnOnlyAvailableMove() {
        Board board = new Board(2, 2);
        board.apply(new Action(Action.Type.HORIZONTAL, 0, 0), 0);
        board.apply(new Action(Action.Type.HORIZONTAL, 1, 0), 1);
        board.apply(new Action(Action.Type.VERTICAL, 0, 0), 0);

        ExpertActionStrategy strategy = new ExpertActionStrategy(new AlphaBetaPruningObserver(),
                new NodeCounterObserver());

        assertEquals(new Action(Action.Type.VERTICAL, 0, 1), strategy.selectAction(board, 1));
    }

    @Test
    void selectActionShouldPreferImmediateClosureWhenAvailable() {
        Board board = new Board(2, 3);
        board.apply(new Action(Action.Type.HORIZONTAL, 0, 0), 0);
        board.apply(new Action(Action.Type.HORIZONTAL, 1, 0), 1);
        board.apply(new Action(Action.Type.VERTICAL, 0, 0), 0);

        ExpertActionStrategy strategy = new ExpertActionStrategy(new AlphaBetaPruningObserver(),
                new NodeCounterObserver());

        assertEquals(new Action(Action.Type.VERTICAL, 0, 1), strategy.selectAction(board, 1));
    }

    @Test
    void getNameShouldReturnExpert() {
        assertEquals("Expert", new ExpertActionStrategy(new AlphaBetaPruningObserver(),
                new NodeCounterObserver()).getName());
    }
}
