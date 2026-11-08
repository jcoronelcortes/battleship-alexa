package jose.coronel.cl.battleship.game.ai;

import jose.coronel.cl.battleship.game.board.Attack;
import jose.coronel.cl.battleship.game.board.BoardGame;
import jose.coronel.cl.battleship.game.board.ShipType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Advanced level: probability density, as in "Battleship Playing Program Utilizing Probability Density
 * Functions" (Boyd, Boyd &amp; Boyd, Kent State University, 2016), based on DataGenetics (Berry, 2011).
 *
 * <p>For every ship still afloat (each one counted separately, so the two 3-cell ships count twice) and
 * every way it could be placed, the cells it would cover get points. Alexa shoots the cell with most
 * points; ties are broken at random.</p>
 * <ul>
 *   <li><b>Hunt mode</b> (no hits on afloat ships): placements over cells not attacked yet.
 *       On an empty board this is the paper's figure 4 (10 in the corners, 34 in the centre).</li>
 *   <li><b>Destroy mode</b> (some hit on an afloat ship): only placements that cover a hit. With one hit
 *       this is the paper's figure 6 (12, 7, 3, 1 away from the hit). With several hits, a placement
 *       that covers more of them is worth {@value #HIT_WEIGHT} times more per extra hit, so Alexa
 *       follows the line of the ship.</li>
 * </ul>
 * <p>Nothing is remembered between turns: the board (misses, hits, sunk ships) is enough.</p>
 */
public class AdvancedStrategy implements AttackStrategy {

    /** Extra weight per additional hit covered by a placement (destroy mode). */
    static final long HIT_WEIGHT = 20;

    private static final int SIZE = BoardGame.NUMBER_OF_ROWS;
    private static final int[][] ORIENTATIONS = {{0, 1}, {1, 0}};

    @Override
    public Attack nextShot(BoardGame playerBoard, Random random) {
        long[][] points = probabilities(playerBoard, false);
        if (allFreeCellsAreZero(playerBoard, points)) {
            // What we deduced about sunk ships contradicts the board (e.g. the player named the wrong
            // ship): forget those deductions rather than shooting blindly
            points = probabilities(playerBoard, true);
        }
        long max = -1;
        List<Attack> best = new ArrayList<>();
        for (int row = 0; row < SIZE; row++) {
            for (int column = 0; column < SIZE; column++) {
                if (playerBoard.wasAttacked(row, column)) {
                    continue;
                }
                if (points[row][column] > max) {
                    max = points[row][column];
                    best.clear();
                }
                if (points[row][column] == max) {
                    best.add(new Attack(row, column));
                }
            }
        }
        return best.get(random.nextInt(best.size()));
    }

    @Override
    public void onHit(BoardGame playerBoard, int row, int column) {
        // Stateless: the next shot is computed from the board
    }

    /** Points of every cell (cells already attacked always have 0). */
    public static long[][] probabilities(BoardGame board) {
        return probabilities(board, false);
    }

    /**
     * @param ignoreSunkDeductions count every ship as afloat and sunk cells as possible ships
     *                             (fallback when the deductions contradict the board)
     */
    static long[][] probabilities(BoardGame board, boolean ignoreSunkDeductions) {
        boolean destroyMode = hasHitsOnAfloatShips(board);
        long[][] points = new long[SIZE][SIZE];
        for (ShipType ship : ShipType.values()) {
            if (!ignoreSunkDeductions && !board.isAfloat(ship)) {
                continue;
            }
            int length = ship.length();
            for (int[] d : ORIENTATIONS) {
                for (int row = 0; row + d[0] * (length - 1) < SIZE; row++) {
                    for (int column = 0; column + d[1] * (length - 1) < SIZE; column++) {
                        int hits = hitsCovered(board, row, column, d, length, ignoreSunkDeductions);
                        if (hits < 0 || (destroyMode && hits == 0)) {
                            continue;
                        }
                        long weight = pow(HIT_WEIGHT, Math.max(0, hits - 1));
                        for (int i = 0; i < length; i++) {
                            int r = row + d[0] * i;
                            int c = column + d[1] * i;
                            if (!board.wasAttacked(r, c)) {
                                points[r][c] += weight;
                            }
                        }
                    }
                }
            }
        }
        return points;
    }

    /** Hits on afloat ships covered by this placement, or -1 if the ship cannot be there. */
    private static int hitsCovered(BoardGame board, int row, int column, int[] d, int length, boolean sunkIsPossible) {
        int hits = 0;
        for (int i = 0; i < length; i++) {
            int r = row + d[0] * i;
            int c = column + d[1] * i;
            if (!board.canHoldShip(r, c) && !(sunkIsPossible && board.isSunk(r, c))) {
                return -1;
            }
            if (board.isImpact(r, c)) {
                hits++;
            }
        }
        return hits;
    }

    private static boolean allFreeCellsAreZero(BoardGame board, long[][] points) {
        for (int row = 0; row < SIZE; row++) {
            for (int column = 0; column < SIZE; column++) {
                if (!board.wasAttacked(row, column) && points[row][column] > 0) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean hasHitsOnAfloatShips(BoardGame board) {
        for (int row = 0; row < SIZE; row++) {
            for (int column = 0; column < SIZE; column++) {
                if (board.isImpact(row, column)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static long pow(long base, int exponent) {
        long result = 1;
        for (int i = 0; i < exponent; i++) {
            result *= base;
        }
        return result;
    }

}
