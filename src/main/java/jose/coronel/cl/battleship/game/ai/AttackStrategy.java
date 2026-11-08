package jose.coronel.cl.battleship.game.ai;

import jose.coronel.cl.battleship.game.board.Attack;
import jose.coronel.cl.battleship.game.board.BoardGame;
import jose.coronel.cl.battleship.game.board.Game;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** How Alexa chooses her shots on the player's board. One implementation per difficulty level. */
public interface AttackStrategy {

    /** Next cell to shoot. Never a cell that was already attacked. */
    Attack nextShot(BoardGame playerBoard, Random random);

    /** Called after the player says Alexa hit a ship at [row, column] (the hit is already recorded). */
    void onHit(BoardGame playerBoard, int row, int column);

    static AttackStrategy forMode(int gameMode) {
        switch (gameMode) {
            case Game.MEDIUM_MODE:
                return new MediumStrategy();
            case Game.ADVANCE_MODE:
                return new AdvancedStrategy();
            default:
                return new BasicStrategy();
        }
    }

    /** Random cell not attacked yet; with checkerboard, only cells where row + column is even (if any left). */
    static Attack randomFreeCell(BoardGame board, Random random, boolean checkerboard) {
        List<Attack> free = new ArrayList<>();
        List<Attack> freeCheckerboard = new ArrayList<>();
        for (int row = 0; row < BoardGame.NUMBER_OF_ROWS; row++) {
            for (int column = 0; column < BoardGame.NUMBER_OF_COLUMNS; column++) {
                if (!board.wasAttacked(row, column)) {
                    free.add(new Attack(row, column));
                    if ((row + column) % 2 == 0) {
                        freeCheckerboard.add(new Attack(row, column));
                    }
                }
            }
        }
        List<Attack> pool = checkerboard && !freeCheckerboard.isEmpty() ? freeCheckerboard : free;
        return pool.get(random.nextInt(pool.size()));
    }

}
