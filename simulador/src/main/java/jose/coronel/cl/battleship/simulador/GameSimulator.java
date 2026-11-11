package jose.coronel.cl.battleship.simulador;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jose.coronel.cl.battleship.game.board.BoardGame;
import jose.coronel.cl.battleship.game.board.Game;
import jose.coronel.cl.battleship.game.board.Ship;
import jose.coronel.cl.battleship.game.board.Zone;
import jose.coronel.cl.battleship.robot.RobotChannels;
import jose.coronel.cl.battleship.session.GameSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * One player against the skill. Builds the same JSON requests Alexa would send, keeps the Alexa session
 * between turns and can answer Alexa's shots automatically using the player's fleet.
 */
@Service
public class GameSimulator {

    /** LOCAL: skill inside the simulator. LAMBDA: deployed Lambda. ALEXA: Alexa's simulator (real language understanding). */
    public enum Mode { LOCAL, LAMBDA, ALEXA }

    /** A line of the conversation. {@code detail}: what Alexa understood (ALEXA mode). */
    public record Line(String who, String text, String detail) {
        public Line(String who, String text) {
            this(who, text, null);
        }
    }

    public record Shot(int row, int column) {
    }

    /** One of Alexa's ships for the scoreboard. Its position is only sent once it is sunk (row -1 before). */
    public record AlexaShip(int ship, String name, int length, boolean sunk, int row, int column, boolean horizontal) {
    }

    /** Everything the page draws. Board cells: 0 not attacked, 1 hit, 2 water, 3 sunk ship. */
    public record State(Mode mode, String robot, boolean sessionOpen, boolean autoAnswer, Integer gameMode,
                        int[][] alexaBoard, boolean[][] alexaShips, int[][] playerShots, int[][] playerFleet,
                        List<PlayerFleet.Placement> ships, boolean fleetLocked, List<AlexaShip> alexaFleet,
                        Shot pendingShot, List<Line> conversation) {
    }

    private static final ObjectMapper JSON = new ObjectMapper();

    private final String skillId;
    private final String locale;
    private final String region;
    private final String lambdaFunction;
    private final IotRobot iotRobot;
    private final Random random = new Random();
    private AlexaSimulatorGateway alexa;

    private Mode mode = Mode.LOCAL;
    private SkillGateway gateway;
    private Map<String, Object> session = new HashMap<>();
    private boolean sessionOpen;
    private boolean autoAnswer = true;
    private PlayerFleet fleet;
    private final List<Line> conversation = new ArrayList<>();

    public GameSimulator(String skillId, String locale, String region, String lambdaFunction,
                         RobotEvents robotEvents, IotRobot iotRobot) {
        this(skillId, locale, region, lambdaFunction, "ask", robotEvents, iotRobot);
    }

    @Autowired
    public GameSimulator(@Value("${simulador.skill-id}") String skillId,
                         @Value("${simulador.locale}") String locale,
                         @Value("${simulador.aws-region}") String region,
                         @Value("${simulador.lambda-function}") String lambdaFunction,
                         @Value("${simulador.ask-cli:ask}") String askCli,
                         RobotEvents robotEvents, IotRobot iotRobot) {
        this.alexa = new AlexaSimulatorGateway(AlexaSimulatorGateway.askCli(askCli), skillId, locale, 700);
        this.skillId = skillId;
        this.locale = locale;
        this.region = region;
        this.lambdaFunction = lambdaFunction;
        this.iotRobot = iotRobot;
        // Local skill: its robot commands go straight to the virtual robot
        RobotChannels.set(command -> {
            try {
                robotEvents.add("local", JSON.writeValueAsString(command));
            } catch (JsonProcessingException e) {
                throw new IllegalStateException(e);
            }
        });
        this.gateway = new LocalSkillGateway();
        this.fleet = PlayerFleet.random(random);
    }

    // ---------- settings ----------

    public synchronized void setMode(Mode newMode) {
        if (newMode == mode) {
            return;
        }
        if (newMode == Mode.LOCAL) {
            gateway = new LocalSkillGateway();
            iotRobot.disconnect();
        } else if (newMode == Mode.LAMBDA) {
            gateway = new LambdaSkillGateway(region, lambdaFunction);
        }
        mode = newMode;
        resetSession();
        conversation.add(new Line("Simulador", switch (newMode) {
            case LOCAL -> "Jugando contra la skill local (sin AWS).";
            case LAMBDA -> "Jugando contra la Lambda en AWS (" + lambdaFunction + ").";
            case ALEXA -> "Alexa completa: tus frases pasan por el reconocimiento de lenguaje de Alexa (unos segundos por turno).";
        }));
    }

    public synchronized void setAutoAnswer(boolean active) {
        autoAnswer = active;
    }

    public synchronized void newFleet() {
        checkFleetCanMove();
        fleet = PlayerFleet.random(random);
    }

    /** Ships placed by hand on the page. */
    public synchronized void setFleet(List<PlayerFleet.Placement> placements) {
        checkFleetCanMove();
        fleet = PlayerFleet.of(placements);
        conversation.add(new Line("Simulador", "Flota ubicada. ¡A jugar!"));
    }

    private void checkFleetCanMove() {
        if (fleetLocked()) {
            throw new IllegalStateException("Alexa ya disparó: no puedes mover tus barcos en esta partida. "
                    + "Comienza un juego nuevo para ubicarlos de nuevo.");
        }
    }

    /** Once Alexa has shot at the player's board, moving the ships would be cheating. */
    private boolean fleetLocked() {
        if (!sessionOpen) {
            return false;
        }
        for (boolean[] row : attackedByAlexa()) {
            for (boolean shot : row) {
                if (shot) {
                    return true;
                }
            }
        }
        return pendingShot() != null;
    }

    // ---------- talking to the skill ----------

    /** "Alexa, abre batalla naval". */
    public synchronized State open() {
        resetSession();
        if (mode == Mode.ALEXA) {
            alexaTurn("abre batalla naval", true);
        } else {
            conversation.add(new Line("Tú", "Alexa, abre batalla naval"));
            send(request("LaunchRequest", null, Map.of(), true));
        }
        return state();
    }

    /**
     * The player says something. In ALEXA mode only the words are used (Alexa decides the intent);
     * in the other modes the intent and slots are sent directly.
     */
    public synchronized State say(String spoken, String intent, Map<String, String> slots) {
        if (mode == Mode.ALEXA) {
            if (!sessionOpen) {
                alexaTurn("abre batalla naval", true);
            }
            alexaTurn(spoken, false);
            return state();
        }
        boolean isNew = !sessionOpen;
        conversation.add(new Line("Tú", spoken));
        send(request("IntentRequest", intent, slots, isNew));
        return state();
    }

    /** Free text typed or transcribed by the browser's speech recognition. */
    public synchronized State sayText(String text) {
        if (mode == Mode.ALEXA) {
            return say(text, null, Map.of());
        }
        if (LocalPhraseParser.isLaunchPhrase(text)) {
            return open();
        }
        LocalPhraseParser.Command command = LocalPhraseParser.parse(text);
        return say(text, command.intent(), command.slots());
    }

    private void alexaTurn(String text, boolean newSession) {
        AlexaSimulatorGateway.Result result;
        try {
            result = alexa.say(text, newSession);
        } catch (RuntimeException e) {
            conversation.add(new Line("Tú", text));
            conversation.add(new Line("Error", e.getMessage()));
            return;
        }
        conversation.add(new Line("Tú", text, understood(result)));
        if (result.skillResponse() != null) {
            handleResponse(result.skillResponse());
        } else {
            conversation.add(new Line(result.intent() == null ? "Error" : "Alexa",
                    result.alexaSays() == null ? "(sin respuesta)" : speechText(result.alexaSays())));
            if (result.intent() == null) {
                sessionOpen = false;
            }
        }
    }

    private static String understood(AlexaSimulatorGateway.Result result) {
        if (result.intent() == null) {
            return "Alexa no entendió la frase";
        }
        StringBuilder text = new StringBuilder("entendió: ").append(result.intent());
        if (!result.slots().isEmpty()) {
            text.append(" · ");
            result.slots().forEach((name, value) -> text.append(name).append('=').append(value).append(' '));
        }
        return text.toString().trim();
    }

    /** Tests replace Alexa's simulator with a fake. */
    void useAlexaSimulator(AlexaSimulatorGateway gateway) {
        this.alexa = gateway;
    }

    /** Answers Alexa's pending shot from the player's fleet ("al agua", "tocado" or "hundiste mi …"). */
    public synchronized State autoAnswer() {
        Shot shot = pendingShot();
        if (shot == null) {
            return state();
        }
        PlayerFleet.Answer answer = fleet.answer(shot.row(), shot.column(), attackedByAlexa());
        return say(answer.spoken(), answer.intent(), answer.slots());
    }

    private void send(String requestJson) {
        String responseJson;
        try {
            responseJson = gateway.send(requestJson);
        } catch (RuntimeException e) {
            conversation.add(new Line("Error", e.getMessage()));
            return;
        }
        handleResponse(responseJson);
    }

    /** Reads a skill response: what Alexa says, whether the session goes on and the game state. */
    private void handleResponse(String responseJson) {
        try {
            JsonNode response = JSON.readTree(responseJson);
            String ssml = response.path("response").path("outputSpeech").path("ssml").asText("");
            conversation.add(new Line("Alexa", speechText(ssml)));
            boolean end = response.path("response").path("shouldEndSession").asBoolean(false);
            JsonNode attributes = response.path("sessionAttributes");
            Map<String, Object> returned = attributes.isObject() ? JSON.convertValue(attributes, Map.class) : null;
            if (end) {
                // Alexa closes the session: keep the final boards on screen, but the next turn starts fresh
                sessionOpen = false;
                lastBoards = returned != null && returned.containsKey("jsonGame") ? returned : session;
                session = new HashMap<>();
            } else {
                sessionOpen = true;
                session = returned != null ? returned : new HashMap<>();
                lastBoards = null;
            }
        } catch (JsonProcessingException e) {
            conversation.add(new Line("Error", "Respuesta inválida de la skill: " + responseJson));
        }
    }

    /** Session of the game that just ended, so its boards stay visible. */
    private Map<String, Object> lastBoards;

    private void resetSession() {
        session = new HashMap<>();
        lastBoards = null;
        sessionOpen = false;
    }

    private String request(String type, String intent, Map<String, String> slots, boolean isNew) {
        ObjectNode envelope = JSON.createObjectNode();
        envelope.put("version", "1.0");
        ObjectNode sessionNode = envelope.putObject("session");
        sessionNode.put("new", isNew).put("sessionId", "simulador-" + UUID.randomUUID());
        sessionNode.putObject("application").put("applicationId", skillId);
        sessionNode.putObject("user").put("userId", "simulador");
        sessionNode.set("attributes", JSON.valueToTree(session));
        ObjectNode system = envelope.putObject("context").putObject("System");
        system.putObject("application").put("applicationId", skillId);
        system.putObject("user").put("userId", "simulador");
        ObjectNode req = envelope.putObject("request");
        req.put("type", type).put("requestId", "simulador-" + UUID.randomUUID())
                .put("locale", locale).put("timestamp", Instant.now().toString());
        if (intent != null) {
            ObjectNode intentNode = req.putObject("intent");
            intentNode.put("name", intent).put("confirmationStatus", "NONE");
            ObjectNode slotsNode = intentNode.putObject("slots");
            slots.forEach((name, value) -> slotsNode.putObject(name)
                    .put("name", name).put("value", value).put("confirmationStatus", "NONE"));
        }
        return envelope.toString();
    }

    static String speechText(String ssml) {
        return ssml.replaceAll("<[^>]+>", " ")
                .replaceAll("\\s+([.,!?])", "$1")
                .replaceAll("([.!?])(\\s*\\.)+", "$1")
                .replaceAll("\\s+", " ").trim();
    }

    // ---------- what the page shows ----------

    public synchronized State state() {
        Game game = currentGame();
        int[][] alexaBoard = new int[9][9];
        boolean[][] alexaShips = new boolean[9][9];
        int[][] playerShots = new int[9][9];
        if (game != null) {
            fill(game.getBoardGameAlexa(), alexaBoard, alexaShips);
            fill(game.getBoardGamePlayer(), playerShots, null);
        }
        return new State(mode, mode == Mode.LOCAL ? "local" : iotRobot.status(), sessionOpen, autoAnswer,
                game == null ? null : game.getGameMode(), alexaBoard, alexaShips, playerShots, fleet.cells(),
                fleet.placements(), fleetLocked(), alexaFleet(game), pendingShot(), List.copyOf(conversation));
    }

    private static List<AlexaShip> alexaFleet(Game game) {
        List<AlexaShip> ships = new ArrayList<>();
        for (int i = 0; i < PlayerFleet.NAMES.length; i++) {
            Ship ship = game == null || game.getShips() == null ? null : game.shipByIdentifier(i);
            boolean sunk = ship != null && ship.getLifePoints() <= 0;
            ships.add(new AlexaShip(i, PlayerFleet.NAMES[i], PlayerFleet.LENGTHS[i], sunk,
                    sunk ? ship.getRow() : -1, sunk ? ship.getColumn() : -1,
                    !sunk || ship.getDirection() == Ship.HORIZONTAL));
        }
        return ships;
    }

    private Game currentGame() {
        Map<String, Object> source = session.containsKey("jsonGame") ? session : lastBoards;
        if (source == null || !(source.get("jsonGame") instanceof String json)) {
            return null;
        }
        return GameSession.fromJson(json);
    }

    private static void fill(BoardGame board, int[][] status, boolean[][] ships) {
        Zone[][] zones = board.getZones();
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                status[r][c] = zones[r][c].getStatus();
                if (ships != null) {
                    ships[r][c] = zones[r][c].isHasShip();
                }
            }
        }
    }

    private Shot pendingShot() {
        Object row = session.get("alexaAttackRow");
        Object column = session.get("alexaAttackColumn");
        return row == null || column == null ? null
                : new Shot(Integer.parseInt(row.toString()), Integer.parseInt(column.toString()));
    }

    private boolean[][] attackedByAlexa() {
        boolean[][] attacked = new boolean[9][9];
        Game game = currentGame();
        if (game != null) {
            Zone[][] zones = game.getBoardGamePlayer().getZones();
            for (int r = 0; r < 9; r++) {
                for (int c = 0; c < 9; c++) {
                    attacked[r][c] = zones[r][c].getStatus() != Zone.PENDING_ATTACK;
                }
            }
        }
        return attacked;
    }

}
