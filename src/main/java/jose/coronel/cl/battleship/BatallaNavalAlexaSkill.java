package jose.coronel.cl.battleship;

import com.amazon.ask.Skill;
import com.amazon.ask.SkillStreamHandler;
import com.amazon.ask.builder.CustomSkillBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jose.coronel.cl.battleship.handlers.common.*;
import jose.coronel.cl.battleship.handlers.game.*;
import jose.coronel.cl.battleship.handlers.exception.GenericExceptionHandler;
import jose.coronel.cl.battleship.robot.LoggingRobotChannel;
import jose.coronel.cl.battleship.robot.RobotChannels;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.slf4j.LoggerFactory.getLogger;

public class BatallaNavalAlexaSkill extends SkillStreamHandler {

    private static final Logger log = getLogger(BatallaNavalAlexaSkill.class);

    static Skill getSkill() {
        return new CustomSkillBuilder()
                .addRequestHandlers(
                        new LaunchRequestHandler(),
                        new TestModeHandler(),
                        new StartGameHandler(),
                        new GameModeHandler(),
                        new PlayerAttackHandler(),
                        new YouFailAlexaHandler(),
                        new ImpactAlexaHandler(),
                        new DestroyedHandler(),
                        new AlexaWinsTheGameHandler(),
                        new HelpIntentHandler(),
                        new CancelAndStopIntentHandler(),
                        new SessionEndedRequestHandler(),
                        new FallbackIntentHandler(),
                        new IntentReflectorHandler())
                .addExceptionHandlers(new GenericExceptionHandler())
                // Only answer requests from our own skill when SKILL_ID is set in Lambda
                .withSkillId(System.getenv("SKILL_ID"))
                .build();
    }

    public BatallaNavalAlexaSkill() {
        super(getSkill());
        if (System.getenv("AWS_LAMBDA_FUNCTION_NAME") != null) {
            prime();
        }
    }

    /**
     * Runs a short fake game during Lambda init so SnapStart's snapshot already has every class loaded.
     * Without this the first real request after a restore takes ~4 s (Alexa gives up at 8 s).
     * The robot is replaced by dummy mode while priming, so nothing reaches the EV3.
     */
    private void prime() {
        long start = System.currentTimeMillis();
        try {
            RobotChannels.set(new LoggingRobotChannel());
            ObjectMapper mapper = new ObjectMapper();
            JsonNode session = mapper.createObjectNode();
            for (String[] request : new String[][]{
                    {"es-US", "ComenzarJuegoIntent", null, null},
                    {"es-US", "ModoDeJuegoIntent", "nivel", "avanzado"},
                    {"es-US", "AtaqueJugadorIntent", "Fila", "C"},
                    {"es-US", "ImpactoAlexaIntent", null, null},
                    {"en-US", "StartGameIntent", null, null}}) {
                session = primeRequest(mapper, session, request);
            }
            log.info("Priming done in {} ms", System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.warn("Priming failed (the skill still works, first request may be slower): {}", e.getMessage());
        } finally {
            // Create the real robot channel now so the IoT client is in the snapshot too
            RobotChannels.set(null);
            RobotChannels.get();
        }
    }

    private JsonNode primeRequest(ObjectMapper mapper, JsonNode session, String[] request) throws Exception {
        ObjectNode envelope = mapper.createObjectNode();
        envelope.put("version", "1.0");
        ObjectNode sessionNode = envelope.putObject("session");
        sessionNode.put("new", false).put("sessionId", "priming");
        sessionNode.set("attributes", session);
        String skillId = System.getenv("SKILL_ID");
        if (skillId != null) {
            sessionNode.putObject("application").put("applicationId", skillId);
            envelope.putObject("context").putObject("System").putObject("application").put("applicationId", skillId);
        } else {
            envelope.putObject("context");
        }
        ObjectNode req = envelope.putObject("request");
        req.put("type", "IntentRequest").put("requestId", "priming").put("locale", request[0])
                .put("timestamp", java.time.Instant.now().toString());
        ObjectNode intent = req.putObject("intent");
        intent.put("name", request[1]);
        ObjectNode slots = intent.putObject("slots");
        if (request[2] != null) {
            slots.putObject(request[2]).put("name", request[2]).put("value", request[3]);
            if ("Fila".equals(request[2])) {
                slots.putObject("Columna").put("name", "Columna").put("value", "cinco");
            }
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        handleRequest(new ByteArrayInputStream(mapper.writeValueAsBytes(envelope)), out, null);
        JsonNode response = mapper.readTree(out.toString(StandardCharsets.UTF_8));
        JsonNode attributes = response.path("sessionAttributes");
        return attributes.isObject() ? attributes : session;
    }

}
