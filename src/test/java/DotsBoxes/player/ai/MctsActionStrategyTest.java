package DotsBoxes.player.ai;

import DotsBoxes.board.Action;
import DotsBoxes.board.Board;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MctsActionStrategyTest {


    @Test
    void selectActionShouldChooseTheOnlyAvailableMove() {
        Board board = new Board(2, 2);
        board.apply(new Action(Action.Type.HORIZONTAL, 0, 0), 0);
        board.apply(new Action(Action.Type.HORIZONTAL, 1, 0), 1);
        board.apply(new Action(Action.Type.VERTICAL, 0, 0), 0);

        MctsActionStrategy strategy = new MctsActionStrategy(1000);
        Action action = strategy.selectAction(board, 1);

        assertEquals(new Action(Action.Type.VERTICAL, 0, 1), action);
    }

    @Test
    void selectActionShouldBeReproducibleWithSameSeed() {
        Board board = new Board(2, 3);
        board.apply(new Action(Action.Type.HORIZONTAL, 0, 0), 0);
        board.apply(new Action(Action.Type.HORIZONTAL, 1, 0), 1);

        MctsActionStrategy s1 = new MctsActionStrategy(1000);
        MctsActionStrategy s2 = new MctsActionStrategy(1000);

        Action a1 = s1.selectAction(board, 0);
        Action a2 = s2.selectAction(board, 0);

        assertEquals(a1, a2);
        assertTrue(board.isValid(a1));
    }

    @Test
    void getNameShouldReturnMcts() {
        assertEquals("MCTS Parallel", new MctsActionStrategy(100).getName());
    }
}
