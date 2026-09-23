package DotsBoxes;

import DotsBoxes.player.Player;
import DotsBoxes.player.automate.AutomatePlayer;
import DotsBoxes.player.automate.GloutonActionStrategy;
import DotsBoxes.player.automate.RandomActionStrategy;
import org.junit.jupiter.api.Test;

import java.util.Random;

public class CompareTest {


    @Test
    void  randomVSglouton() {
        int wRandom=0, wGlouton=0, draw=0;
        int nbTest = 100;

        for(int i = 0; i < nbTest; i++){
            Random rand = new Random();
            int Tgrille = rand.nextInt(11) + 3;

            RandomActionStrategy random = new RandomActionStrategy();
            GloutonActionStrategy glouton = new GloutonActionStrategy();

            AutomatePlayer p1 = new AutomatePlayer(0, random);
            AutomatePlayer p2 = new AutomatePlayer(1, glouton);

            DotsBoxesGame game = new DotsBoxesGame(Tgrille, Tgrille, p1, p2);

            Player winner = game.play();
            if (winner == null) draw++;
            else if (winner.getId() == 0) wRandom++;
            else wGlouton++;
        }

        System.out.println("Random vs Glouton sur " + nbTest + " parties");
        System.out.println("Random : " + wRandom + " victoires");
        System.out.println("Glouton: " + wGlouton + " victoires");
        System.out.println("Nuls   : " + draw);
    }
}
