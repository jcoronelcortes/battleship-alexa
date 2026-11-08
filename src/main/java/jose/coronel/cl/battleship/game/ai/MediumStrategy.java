package jose.coronel.cl.battleship.game.ai;

import jose.coronel.cl.battleship.game.board.Attack;
import jose.coronel.cl.battleship.game.board.BoardGame;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Medium level, "hunt and target":
 * hunt on a checkerboard pattern (every ship covers at least one of those cells), and after a hit
 * target the four neighbours, preferring the line of the ship once two hits are aligned.
 */
public class MediumStrategy implements AttackStrategy {

    /** Up, down, left, right (the order the candidates are added). */
    private static final int[][] NEIGHBOURS = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

    @Override
    public Attack nextShot(BoardGame playerBoard, Random random) {
        Attack target = playerBoard.removeNextAttack(random);
        return target != null ? target : AttackStrategy.randomFreeCell(playerBoard, random, true);
    }

    @Override
    public void onHit(BoardGame playerBoard, int row, int column) {
        List<Attack> neighbours = new ArrayList<>();
        for (int[] d : NEIGHBOURS) {
            int r = row + d[0];
            int c = column + d[1];
            if (r >= 0 && r < BoardGame.NUMBER_OF_ROWS && c >= 0 && c < BoardGame.NUMBER_OF_COLUMNS
                    && !playerBoard.wasAttacked(r, c)) {
                neighbours.add(new Attack(r, c));
            }
        }
        playerBoard.addNewNextAttacks(neighbours);
    }

}
