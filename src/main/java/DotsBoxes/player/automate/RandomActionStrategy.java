package DotsBoxes.player.automate;

import DotsBoxes.board.Action;
import DotsBoxes.board.Board;
import DotsBoxes.player.ActionStrategy;

import java.util.List;
import java.util.Random;

/**
 * Version etudiants.
 * TODO:
 * - Construire la liste des actions valides.
 * - Tirer une action aleatoirement.
 * - Retourner null si aucun coup n'est disponible.
 */
public class RandomActionStrategy implements ActionStrategy {

    @Override
    public Action selectAction(Board board, int playerId) {
        List<Action> actionsAvailable = board.getAvailableActions();
        Random random = new Random();
        if (actionsAvailable.isEmpty()) {
            return null;
        } return actionsAvailable.get(random.nextInt(actionsAvailable.size()));
    }

    @Override
    public String getName() {
        return "Random";
    }
}
