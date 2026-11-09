package jose.coronel.cl.battleship.simulador;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jose.coronel.cl.battleship.game.board.BoardGame;
import jose.coronel.cl.battleship.game.board.Game;
import jose.coronel.cl.battleship.game.board.Zone;
import jose.coronel.cl.battleship.robot.RobotChannels;
import jose.coronel.cl.battleship.session.GameSession;
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

    public enum Mode { LOCAL, LAMBDA }

    public record Line(String who, String text) {
    }

    public record Shot(int row, int column) {
    }

    /** Everything the page draws. Board cells: 0 not attacked, 1 hit, 2 water, 3 sunk ship. */
    public record State(Mode mode, String robot, boolean sessionOpen, boolean autoAnswer, Integer gameMode,
                        int[][] alexaBoard, boolean[][] alexaShips, int[][] playerShots, int[][] playerFleet,
                        List<PlayerFleet.Placement> ships, boolean fleetLocked,
                        Shot pendingShot, List<Line> conversation) {
    }

    private static final ObjectMapper JSON = new ObjectMapper();

    private final String skillId;
    private final String locale;
    private final String region;
    private final String lambdaFunction;
    private final IotRobot iotRobot;
    private final Random random = new Random();

    private Mode mode = Mode.LOCAL;
    private SkillGateway gateway;
    private Map<String, Object> session = new HashMap<>();
    private boolean sessionOpen;
    private boolean autoAnswer = true;
    private PlayerFleet fleet;
    private final List<Line> conversation = new ArrayList<>();

    public GameSimulator(@Value("${simulador.skill-id}") String skillId,
                         @Value("${simulador.locale}") String locale,
                         @Value("${simulador.aws-region}") String region,
                         @Value("${simulador.lambda-function}") String lambdaFunction,
                         RobotEvents robotEvents, IotRobot iotRobot) {
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
        gateway = newMode == Mode.LOCAL ? new LocalSkillGateway() : new LambdaSkillGateway(region, lambdaFunction);
        if (newMode == Mode.LOCAL) {
            iotRobot.disconnect();
        }
        mode = newMode;
        resetSession();
        conversation.add(new Line("Simulador", newMode == Mode.LOCAL
                ? "Jugando contra la skill local (sin AWS)."
                : "Jugando contra la Lambda en AWS (" + lambdaFunction + ")."));
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
        conversation.add(new Line("Tú", "Alexa, abre batalla naval"));
        send(request("LaunchRequest", null, Map.of(), true));
        return state();
    }

    public synchronized State say(String spoken, String intent, Map<String, String> slots) {
        boolean isNew = !sessionOpen;
        conversation.add(new Line("Tú", spoken));
        send(request("IntentRequest", intent, slots, isNew));
        return state();
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
                fleet.placements(), fleetLocked(), pendingShot(), List.copyOf(conversation));
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
