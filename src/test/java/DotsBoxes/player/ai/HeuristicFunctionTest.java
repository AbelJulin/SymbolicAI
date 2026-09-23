package DotsBoxes.player.ai;

import DotsBoxes.board.Action;
import DotsBoxes.board.Board;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HeuristicFunctionTest {
    HeuristicFunction h = new HeuristicFunction();

    @Test
    void winningPlayer() {
        Board board = new Board(2, 2);
        // creation d'une case fermee pour joueur 0
        board.apply(new Action(Action.Type.HORIZONTAL, 0, 0), 0);
        board.apply(new Action(Action.Type.HORIZONTAL, 1, 0), 0);
        board.apply(new Action(Action.Type.VERTICAL, 0, 0), 0);
        board.apply(new Action(Action.Type.VERTICAL, 0, 1), 0);

        assertTrue(h.evaluate(board, 0) > 0);
        assertTrue(h.evaluate(board, 1) < 0);
    }
}