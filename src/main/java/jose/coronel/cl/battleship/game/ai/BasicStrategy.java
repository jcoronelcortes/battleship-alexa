package jose.coronel.cl.battleship.game.ai;

import jose.coronel.cl.battleship.game.board.Attack;
import jose.coronel.cl.battleship.game.board.BoardGame;

import java.util.Random;

/** Basic level: random shots, no memory. */
public class BasicStrategy implements AttackStrategy {

    @Override
    public Attack nextShot(BoardGame playerBoard, Random random) {
        return AttackStrategy.randomFreeCell(playerBoard, random, false);
    }

    @Override
    public void onHit(BoardGame playerBoard, int row, int column) {
        // no memory
    }

}
