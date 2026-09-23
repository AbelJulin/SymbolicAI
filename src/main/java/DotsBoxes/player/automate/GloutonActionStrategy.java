package DotsBoxes.player.automate;

import DotsBoxes.board.Action;
import DotsBoxes.board.Board;
import DotsBoxes.player.ActionStrategy;

import java.util.List;
import java.util.Random;

/**
 * Version etudiants.
 * TODO:
 * - Implementer une strategie gloutonne:
 *   1) si un coup ferme une ou plusieurs boxes, le jouer
 *   2) sinon, jouer un coup aleatoire valide
 * - Retourner null si aucun coup n'est disponible.
 */
public class GloutonActionStrategy implements ActionStrategy {
    private final Random random;
    public GloutonActionStrategy() {
        random = new Random();
    }

    public GloutonActionStrategy(long seed) {
        random = new Random(seed);
    }

    @Override
    public Action selectAction(Board board, int playerId) {
        List<Action> actionsAvailable = board.getAvailableActions();
        for (Action action : actionsAvailable) {
            if (board.isActionClosing(action) ) { return action;}
        }
        if (actionsAvailable.isEmpty()) {
            return null;
        } return actionsAvailable.get(random.nextInt(actionsAvailable.size()));
    }

    @Override
    public String getName() {
        return "Glouton";
    }
}
