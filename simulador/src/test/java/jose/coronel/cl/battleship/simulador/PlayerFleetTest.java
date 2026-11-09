package jose.coronel.cl.battleship.simulador;

import jose.coronel.cl.battleship.simulador.PlayerFleet.Placement;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Ships placed by hand on the page. */
class PlayerFleetTest {

    /** A valid fleet: one ship per row, all horizontal. */
    private static List<Placement> validFleet() {
        return new ArrayList<>(List.of(
                new Placement(0, 0, 0, true),
                new Placement(1, 2, 0, true),
                new Placement(2, 4, 0, true),
                new Placement(3, 6, 0, true),
                new Placement(4, 8, 0, true)));
    }

    @Test
    void validFleetIsAccepted() {
        PlayerFleet fleet = PlayerFleet.of(validFleet());
        assertEquals(4, fleet.cells()[8][4], "the aircraft carrier covers I1-I5");
        assertEquals(-1, fleet.cells()[8][5]);
        assertEquals(5, fleet.placements().size());
    }

    @Test
    void verticalShipsAreSupported() {
        List<Placement> fleet = validFleet();
        fleet.set(4, new Placement(4, 0, 8, false)); // aircraft carrier A9-E9
        assertEquals(4, PlayerFleet.of(fleet).cells()[4][8]);
    }

    @Test
    void shipOutsideTheBoardIsRejected() {
        List<Placement> fleet = validFleet();
        fleet.set(4, new Placement(4, 8, 6, true)); // I7-I11
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> PlayerFleet.of(fleet));
        assertEquals("El portaaviones se sale del tablero.", e.getMessage());
    }

    @Test
    void overlappingShipsAreRejected() {
        List<Placement> fleet = validFleet();
        fleet.set(1, new Placement(1, 0, 1, false)); // submarino on top of the destructor
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> PlayerFleet.of(fleet));
        assertTrue(e.getMessage().contains("se superpone"), e.getMessage());
    }

    @Test
    void incompleteOrRepeatedFleetIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> PlayerFleet.of(validFleet().subList(0, 4)));
        List<Placement> repeated = validFleet();
        repeated.set(4, new Placement(0, 8, 0, true));
        assertThrows(IllegalArgumentException.class, () -> PlayerFleet.of(repeated));
    }

    @Test
    void fleetCannotMoveAfterAlexaShoots() {
        RobotEvents robot = new RobotEvents();
        GameSimulator simulator = new GameSimulator("amzn1.ask.skill.test", "es-US", "us-east-1", "fn", robot,
                new IotRobot(robot, "no-existe"));
        simulator.setFleet(validFleet()); // allowed before the game
        simulator.open();
        simulator.say("comenzar juego", "ComenzarJuegoIntent", Map.of());
        simulator.setFleet(validFleet()); // still allowed: Alexa has not shot
        GameSimulator.State state = simulator.say("A uno", "AtaqueJugadorIntent", Map.of("Fila", "A", "Columna", "1"));
        assertTrue(state.fleetLocked());
        assertThrows(IllegalStateException.class, () -> simulator.setFleet(validFleet()));
        assertThrows(IllegalStateException.class, simulator::newFleet);
    }

}
