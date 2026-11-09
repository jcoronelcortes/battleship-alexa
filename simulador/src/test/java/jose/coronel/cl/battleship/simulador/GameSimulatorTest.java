package jose.coronel.cl.battleship.simulador;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Plays against the local skill (no AWS) through the simulator, like the web page does. */
class GameSimulatorTest {

    private RobotEvents robot;
    private GameSimulator simulator;

    @BeforeEach
    void setUp() {
        robot = new RobotEvents();
        simulator = new GameSimulator("amzn1.ask.skill.test", "es-US", "us-east-1", "battleship-alexa:live",
                robot, new IotRobot(robot, "no-existe"));
    }

    @Test
    void fullTurnWithAutomaticAnswer() {
        simulator.open();
        simulator.say("comenzar juego", "ComenzarJuegoIntent", Map.of());
        simulator.say("modo de juego avanzado", "ModoDeJuegoIntent", Map.of("nivel", "avanzado"));
        GameSimulator.State afterAttack = simulator.say("C cinco", "AtaqueJugadorIntent", Map.of("Fila", "C", "Columna", "5"));

        assertTrue(afterAttack.alexaBoard()[2][4] != 0, "C5 is marked on Alexa's board");
        assertTrue(afterAttack.alexaFleet().stream().allMatch(ship -> ship.sunk() || ship.row() == -1),
                "afloat ships do not reveal their position");
        assertNotNull(afterAttack.pendingShot(), "Alexa shot back");
        assertEquals(2, afterAttack.gameMode());

        GameSimulator.State afterAnswer = simulator.autoAnswer();
        assertNull(afterAnswer.pendingShot(), "the shot was answered");
        GameSimulator.Shot shot = afterAttack.pendingShot();
        assertTrue(afterAnswer.playerShots()[shot.row()][shot.column()] != 0, "Alexa's shot is on the player board");

        List<String> commands = robot.since(0).stream()
                .map(e -> command(e.json())).toList();
        assertEquals(List.of("start_game", "game_mode_configured", "player_attack"), commands.subList(0, 3));
        assertTrue(List.of("fail_attack", "alexa_impact", "alexa_ship_destroyed").contains(commands.get(3)));
    }

    private static String command(String json) {
        try {
            return new ObjectMapper().readTree(json).path("command").asText();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void conversationShowsAlexaTextWithoutSsml() {
        GameSimulator.State state = simulator.open();
        String alexa = state.conversation().get(state.conversation().size() - 1).text();
        assertTrue(alexa.startsWith("Batalla naval"), alexa);
        assertTrue(!alexa.contains("<"), alexa);
    }

    @Test
    void winningShotStaysOnTheBoardAfterTheGameEnds() {
        simulator.open();
        GameSimulator.State state = simulator.say("comenzar juego", "ComenzarJuegoIntent", Map.of());
        boolean[][] ships = state.alexaShips();
        outer:
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                if (ships[r][c]) {
                    state = simulator.say("x", "AtaqueJugadorIntent",
                            Map.of("Fila", String.valueOf("ABCDEFGHI".charAt(r)), "Columna", String.valueOf(c + 1)));
                    if (!state.sessionOpen()) {
                        break outer;
                    }
                    state = simulator.autoAnswer();
                    if (!state.sessionOpen()) {
                        break outer;
                    }
                }
            }
        }
        long hits = java.util.Arrays.stream(state.alexaBoard()).flatMapToInt(java.util.Arrays::stream).filter(v -> v == 1).count();
        String last = state.conversation().get(state.conversation().size() - 1).text();
        assertTrue(state.alexaFleet().stream().allMatch(GameSimulator.AlexaShip::sunk), "all Alexa ships sunk");
        assertTrue(state.alexaFleet().stream().allMatch(ship -> ship.row() >= 0), "positions revealed once sunk");
        // Alexa cannot sink all 17 cells in her first 17 shots, so the player always wins here
        assertTrue(last.contains("Tu ganas"), last);
        assertEquals(17, hits, "all 17 hits shown, including the winning one");
        assertTrue(!state.sessionOpen());
    }

    @Test
    void speechTextCleansSsml() {
        assertEquals("Ataque finalizado. Es mi turno.",
                GameSimulator.speechText("Ataque finalizado. <break time=\"1s\"/>. Es mi turno<break time=\"1s\"/>."));
    }

    @Test
    void fleetAnswersLikeThePlayer() {
        PlayerFleet fleet = PlayerFleet.random(new Random(3));
        int[][] cells = fleet.cells();
        boolean[][] attacked = new boolean[9][9];
        int shipCells = 0;
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                if (cells[r][c] >= 0) {
                    shipCells++;
                }
            }
        }
        assertEquals(17, shipCells);
        // Shoot every cell of the destroyer (index 0): first "tocado", then "hundiste mi destructor"
        List<String> answers = new java.util.ArrayList<>();
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                if (cells[r][c] == 0) {
                    answers.add(fleet.answer(r, c, attacked).spoken());
                    attacked[r][c] = true;
                }
            }
        }
        assertEquals(List.of("tocado", "hundiste mi destructor"), answers);
    }

}
