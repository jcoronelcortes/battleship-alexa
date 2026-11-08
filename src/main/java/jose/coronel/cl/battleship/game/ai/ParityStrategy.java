package jose.coronel.cl.battleship.game.ai;

import jose.coronel.cl.battleship.game.board.Attack;
import jose.coronel.cl.battleship.game.board.BoardGame;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

public class ParityStrategy {

    private static Logger log = getLogger(ParityStrategy.class);


    public static void addNewNextAttacks(BoardGame playerBoardGame, int row, int column){
        List<Attack> newNextAttacks = new ArrayList<>();
        if((row - 1) >= 0 && !playerBoardGame.wasAttacked(row - 1 , column)){
            newNextAttacks.add(new Attack(row - 1, column));
        }
        if((row + 1) < 9 && !playerBoardGame.wasAttacked(row + 1 , column)){
            newNextAttacks.add(new Attack(row + 1, column));
        }
        if((column - 1) >= 0 && !playerBoardGame.wasAttacked(row , column - 1)){
            newNextAttacks.add(new Attack(row, column - 1));
        }
        if((column + 1) < 9 && !playerBoardGame.wasAttacked(row , column + 1)){
            newNextAttacks.add(new Attack(row, column + 1));
        }

        playerBoardGame.addNewNextAttacks(newNextAttacks);
        log.info("Next attacks : {}", newNextAttacks);
    }
}
