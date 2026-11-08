package jose.coronel.cl.battleship.game.ai;

import jose.coronel.cl.battleship.game.board.BoardGame;
import jose.coronel.cl.battleship.game.board.ShipType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the advanced AI against the figures of "Battleship Playing Program Utilizing Probability
 * Density Functions" (Kent State University, 2016). The paper uses a 10x10 board; on our 9x9 board the
 * cells far enough from the edges have the same values.
 */
class AdvancedStrategyPaperTest {

    @Test
    void figure4HuntModeSumsEveryShip() {
        long[][] points = AdvancedStrategy.probabilities(BoardGame.empty());
        // Corner: each of the 5 ships fits 1 way horizontally and 1 vertically: 5 x 2 = 10, as in the paper
        assertEquals(10, points[0][0]);
        // Centre: every ship fits "length" ways in each direction: 2 x (2+3+3+4+5) = 34, as in the paper
        assertEquals(34, points[4][4]);
    }

    @Test
    void figure6DestroyModeAroundASingleHit() {
        BoardGame board = BoardGame.empty();
        board.recordImpact(4, 4);
        long[][] points = AdvancedStrategy.probabilities(board);
        // Paper: 12, 7, 3, 1 going away from the hit, 0 elsewhere
        assertEquals(12, points[4][5]);
        assertEquals(7, points[4][6]);
        assertEquals(3, points[4][7]);
        assertEquals(1, points[4][8]);
        assertEquals(12, points[3][4]);
        assertEquals(0, points[3][3]);
        assertEquals(0, points[4][4], "the hit itself is not a target");
    }

    @Test
    void twoHitsInARowFollowTheLine() {
        BoardGame board = BoardGame.empty();
        board.recordImpact(4, 4);
        board.recordImpact(4, 5);
        long[][] points = AdvancedStrategy.probabilities(board);
        assertTrue(points[4][3] > 10 * points[3][4], "cells in line with both hits must dominate");
        assertTrue(points[4][6] > 10 * points[5][5]);
    }

    @Test
    void sunkShipCellsAreNotPossibleShips() {
        BoardGame board = BoardGame.empty();
        board.recordImpact(0, 0);
        board.markSunk(ShipType.DESTROYER, 0, 1);
        long[][] points = AdvancedStrategy.probabilities(board);
        // Back to hunt mode without the destroyer. Cell (1,0), under the sunk ship: each of the 4 afloat
        // ships fits 1 way horizontally (starting at column 0) and 1 way vertically (starting at row 1,
        // because row 0 is the sunk destroyer): 4 x 2 = 8
        assertEquals(0, points[0][0]);
        assertEquals(8, points[1][0]);
    }

    @Test
    void touchingShipsAreNotGuessed() {
        // Hits at (0,0) (0,1), destroyer sunk with the shot at (0,2): the only 2-cell line of hits that
        // contains (0,2) is (0,1)-(0,2), so it is marked sunk; (0,0) belongs to another ship and stays a hit
        BoardGame board = BoardGame.empty();
        board.recordImpact(0, 0);
        board.recordImpact(0, 1);
        board.markSunk(ShipType.DESTROYER, 0, 2);
        assertTrue(board.isSunk(0, 1) && board.isSunk(0, 2));
        assertTrue(board.isImpact(0, 0), "the other ship's hit is kept");

        // Hits (2,3) (2,4) (2,5) and a destroyer sunk at (2,4): (2,3)-(2,4) or (2,4)-(2,5)? Ambiguous: keep the hits
        BoardGame ambiguous = BoardGame.empty();
        ambiguous.recordImpact(2, 3);
        ambiguous.recordImpact(2, 5);
        ambiguous.markSunk(ShipType.DESTROYER, 2, 4);
        assertTrue(ambiguous.isImpact(2, 3) && ambiguous.isImpact(2, 4) && ambiguous.isImpact(2, 5));
        assertEquals(4, ambiguous.pendingShips());
    }

    @Test
    void hitsOnAnotherShipAreKeptAfterASink() {
        // A destroyer at row 0 is sunk while another ship was already hit at (2, 4)
        BoardGame board = BoardGame.empty();
        board.recordImpact(2, 4);
        board.recordImpact(0, 0);
        board.markSunk(ShipType.DESTROYER, 0, 1);
        long[][] points = AdvancedStrategy.probabilities(board);
        assertTrue(points[2][5] > 0 && points[1][4] > 0, "still hunting the other ship around (2, 4)");
        assertEquals(0, points[5][5], "destroy mode: cells far from the hit get nothing");
    }

}
