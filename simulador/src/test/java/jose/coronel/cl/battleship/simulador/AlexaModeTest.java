package jose.coronel.cl.battleship.simulador;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * "Alexa completa" mode with a fake ASK CLI that replays real answers captured from Alexa's simulator
 * (src/test/resources/alexa-sim, without tokens or account ids).
 */
class AlexaModeTest {

    private final List<List<String>> calls = new ArrayList<>();
    private final Map<String, String> answers = Map.of(
            "abre batalla naval", fixture("launch"),
            "comenzar juego", fixture("start"),
            "pizza con queso", fixture("unknown"));
    private String lastPhrase;
    private GameSimulator simulator;

    /** Fake ASK CLI: simulate-skill remembers the phrase, get-skill-simulation returns its captured result. */
    private String cli(List<String> arguments) {
        calls.add(arguments);
        if (arguments.get(1).equals("simulate-skill")) {
            lastPhrase = arguments.get(arguments.indexOf("--input-content") + 1);
            return "{\"id\":\"sim-1\",\"status\":\"IN_PROGRESS\"}";
        }
        return answers.getOrDefault(lastPhrase, "{\"status\":\"FAILED\",\"result\":{\"error\":{\"message\":"
                + "\"This utterance did not resolve to any intent in your skill.\"}}}");
    }

    @BeforeEach
    void setUp() {
        RobotEvents robot = new RobotEvents();
        simulator = new GameSimulator("amzn1.ask.skill.test", "es-US", "us-east-1", "fn", robot, new IotRobot(robot, "no-existe"));
        simulator.useAlexaSimulator(new AlexaSimulatorGateway(this::cli, "amzn1.ask.skill.test", "es-US", 0));
        simulator.setMode(GameSimulator.Mode.ALEXA);
    }

    @Test
    void phrasesGoThroughAlexaAndShowWhatSheUnderstood() {
        simulator.open();
        GameSimulator.State state = simulator.sayText("comenzar juego");

        assertTrue(state.sessionOpen());
        assertNotNull(state.gameMode(), "the game comes from the skill response in the simulation");
        GameSimulator.Line me = state.conversation().stream().filter(l -> l.text().equals("comenzar juego")).findFirst().orElseThrow();
        assertEquals("entendió: ComenzarJuegoIntent", me.detail());
        String alexa = state.conversation().get(state.conversation().size() - 1).text();
        assertTrue(alexa.startsWith("Voy a preparar el tablero"), alexa);

        // The first phrase opens a new session; the next ones continue it
        assertEquals("FORCE_NEW_SESSION", calls.get(0).get(calls.get(0).indexOf("--session-mode") + 1));
        assertEquals("DEFAULT", calls.get(2).get(calls.get(2).indexOf("--session-mode") + 1));
    }

    @Test
    void unknownPhraseGoesToFallback() {
        simulator.open();
        GameSimulator.State state = simulator.sayText("pizza con queso");
        GameSimulator.Line me = state.conversation().stream().filter(l -> l.text().equals("pizza con queso")).findFirst().orElseThrow();
        assertEquals("entendió: AMAZON.FallbackIntent", me.detail());
        assertTrue(state.conversation().get(state.conversation().size() - 1).text().startsWith("No puedo entender"));
    }

    @Test
    void phraseThatReachesNoIntentIsExplained() {
        simulator.open();
        GameSimulator.State state = simulator.sayText("qué hora es");
        GameSimulator.Line last = state.conversation().get(state.conversation().size() - 1);
        assertEquals("Error", last.who());
        assertTrue(last.text().contains("no relacionó esa frase"), last.text());
        assertTrue(!state.sessionOpen());
    }

    @Test
    void parsesSlotsFromTheSimulation() throws IOException {
        String simulation = """
                {"status":"SUCCESSFUL","result":{"alexaExecutionInfo":{"consideredIntents":[{"name":"AtaqueJugadorIntent",
                "slots":{"Fila":{"name":"Fila","value":"C"},"Columna":{"name":"Columna","value":"5"},"Otro":{"name":"Otro"}}}]},
                "skillExecutionInfo":{"invocations":[]}}}""";
        AlexaSimulatorGateway.Result result = AlexaSimulatorGateway.parse(new ObjectMapper().readTree(simulation));
        assertEquals("AtaqueJugadorIntent", result.intent());
        assertEquals(Map.of("Fila", "C", "Columna", "5"), result.slots());
        assertNull(result.skillResponse());
    }

    private static String fixture(String name) {
        try (InputStream in = AlexaModeTest.class.getResourceAsStream("/alexa-sim/" + name + ".json")) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

}
