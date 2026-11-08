package jose.coronel.cl.battleship.game.board;

import jose.coronel.cl.battleship.util.Coordinates;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameSetupTest {

    @Test
    void alexaFleetIsValidAndUsesBothDirections() {
        // Before 2026 the direction was random.nextInt(1), which is always 0: every ship was horizontal
        Random random = new Random(7);
        int vertical = 0;
        int horizontal = 0;
        for (int i = 0; i < 500; i++) {
            Game game = Game.newGame(random);
            int cells = 0;
            for (int row = 0; row < 9; row++) {
                for (int column = 0; column < 9; column++) {
                    if (game.getBoardGameAlexa().hasShip(row, column)) {
                        cells++;
                    }
                }
            }
            assertEquals(ShipType.TOTAL_CELLS, cells, "ships overlap or are missing");
            for (Ship ship : game.getShips()) {
                if (ship.getDirection() == Ship.VERTICAL) {
                    vertical++;
                } else {
                    horizontal++;
                }
            }
        }
        assertTrue(vertical > 500 && horizontal > 500, "vertical=" + vertical + " horizontal=" + horizontal);
    }

    @Test
    void shipNamesInBothLanguages() {
        assertEquals(ShipType.AIRCRAFT_CARRIER, ShipType.fromSpoken("porta aviones").orElseThrow());
        assertEquals(ShipType.AIRCRAFT_CARRIER, ShipType.fromSpoken("Aircraft Carrier").orElseThrow());
        assertEquals(ShipType.SUBMARINE, ShipType.fromSpoken("SUBMARINO").orElseThrow());
        assertEquals("shipNameAircraftCarrier", ShipType.AIRCRAFT_CARRIER.nameKey());
        assertTrue(ShipType.fromSpoken("barco").isEmpty());
    }

    @Test
    void coordinatesAsAlexaHearsThem() {
        assertEquals(2, Coordinates.rowFromSpoken("c"));
        assertEquals(1, Coordinates.rowFromSpoken("B."));
        assertEquals(8, Coordinates.rowFromSpoken("Y"));
        assertEquals(-1, Coordinates.rowFromSpoken("Z"));
        assertEquals(4, Coordinates.columnFromSpoken("cinco"));
        assertEquals(4, Coordinates.columnFromSpoken("5"));
        assertEquals(8, Coordinates.columnFromSpoken("nine"));
        assertEquals(-1, Coordinates.columnFromSpoken("diez"));
        assertEquals("I", Coordinates.rowLetter(8));
    }

}
