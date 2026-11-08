package jose.coronel.cl.battleship.session;

import com.amazon.ask.attributes.AttributesManager;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jose.coronel.cl.battleship.game.board.Attack;
import jose.coronel.cl.battleship.game.board.Game;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Optional;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * The game lives in the Alexa session between turns (as JSON), together with Alexa's last shot
 * while the player has not answered it yet.
 */
public class GameSession {

    private static final Logger log = getLogger(GameSession.class);
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    static final String GAME = "jsonGame";
    static final String ALEXA_ROW = "alexaAttackRow";
    static final String ALEXA_COLUMN = "alexaAttackColumn";

    private final AttributesManager attributes;

    public GameSession(AttributesManager attributes) {
        this.attributes = attributes;
    }

    /** The current game, or empty if the player did not start one in this session. */
    public Optional<Game> game() {
        Object json = attributes().get(GAME);
        if (!(json instanceof String)) {
            return Optional.empty();
        }
        try {
            return Optional.of(fromJson((String) json));
        } catch (IllegalArgumentException e) {
            log.error("Could not read the game from the session: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public void save(Game game) {
        put(GAME, toJson(game));
    }

    /** The game as it is stored in the session. */
    public static String toJson(Game game) {
        try {
            return MAPPER.writeValueAsString(game);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not write the game as JSON", e);
        }
    }

    public static Game fromJson(String json) {
        try {
            return MAPPER.readValue(json, Game.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid game JSON: " + e.getOriginalMessage(), e);
        }
    }

    /** Alexa's last shot, waiting for the player to say "you fail", "impact" or "sunk". */
    public Optional<Attack> pendingAlexaShot() {
        Object row = attributes().get(ALEXA_ROW);
        Object column = attributes().get(ALEXA_COLUMN);
        if (row == null || column == null) {
            return Optional.empty();
        }
        return Optional.of(new Attack(Integer.parseInt(row.toString()), Integer.parseInt(column.toString())));
    }

    public void setPendingAlexaShot(Attack shot) {
        put(ALEXA_ROW, String.valueOf(shot.getRow()));
        put(ALEXA_COLUMN, String.valueOf(shot.getColumn()));
    }

    public void clearPendingAlexaShot() {
        Map<String, Object> values = attributes();
        values.remove(ALEXA_ROW);
        values.remove(ALEXA_COLUMN);
        attributes.setSessionAttributes(values);
    }

    private Map<String, Object> attributes() {
        return attributes.getSessionAttributes();
    }

    private void put(String key, Object value) {
        Map<String, Object> values = attributes();
        values.put(key, value);
        attributes.setSessionAttributes(values);
    }

}
