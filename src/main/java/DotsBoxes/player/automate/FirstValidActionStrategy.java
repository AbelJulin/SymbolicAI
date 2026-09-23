package DotsBoxes.player.automate;

import DotsBoxes.board.Action;
import DotsBoxes.board.Board;
import DotsBoxes.player.ActionStrategy;

import java.util.List;

/**
 * Version etudiant.
 * TODO:
 * - Parcourir le plateau.
 * - Retourner le premier segment libre.
 * - Retourner null si aucun coup n'est possible.
 */
public class FirstValidActionStrategy implements ActionStrategy {

    @Override
    public Action selectAction(Board board, int playerId) {
        List<Action> actionsAvailable = board.getAvailableActions();
        if (actionsAvailable.isEmpty()) {
            return null;
        } return actionsAvailable.get(0);
    }

    @Override
    public String getName() {
        return "First valid";
    }
}
