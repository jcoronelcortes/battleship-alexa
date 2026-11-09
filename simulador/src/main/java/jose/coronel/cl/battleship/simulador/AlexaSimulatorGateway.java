package jose.coronel.cl.battleship.simulador;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * "Alexa completa" mode: sends the player's words as text to Alexa's official simulator
 * (ask smapi simulate-skill). Alexa's own language understanding picks the intent and slots and then
 * calls the deployed Lambda, exactly as with a real Echo. Uses the ASK CLI login (ask configure).
 */
public class AlexaSimulatorGateway {

    /** Runs the ASK CLI and returns its standard output. Replaceable in tests. */
    public interface Cli {
        String run(List<String> arguments);
    }

    /**
     * One turn.
     * @param intent       intent Alexa understood (null if the words reached no intent)
     * @param slots        slot values Alexa heard
     * @param skillResponse the skill's response JSON (same as a Lambda response), or null if the skill was not called
     * @param alexaSays    what Alexa said when the skill was not called (or an error)
     */
    public record Result(String intent, Map<String, String> slots, String skillResponse, String alexaSays) {
    }

    private static final ObjectMapper JSON = new ObjectMapper();

    private final Cli cli;
    private final String skillId;
    private final String locale;
    private final long pollMillis;

    public AlexaSimulatorGateway(Cli cli, String skillId, String locale, long pollMillis) {
        this.cli = cli;
        this.skillId = skillId;
        this.locale = locale;
        this.pollMillis = pollMillis;
    }

    public Result say(String text, boolean newSession) {
        JsonNode started = json(cli.run(List.of("smapi", "simulate-skill", "--skill-id", skillId,
                "--input-content", text, "--device-locale", locale,
                "--session-mode", newSession ? "FORCE_NEW_SESSION" : "DEFAULT")));
        String simulationId = started.path("id").asText();
        for (int attempt = 0; attempt < 60; attempt++) {
            sleep(pollMillis);
            JsonNode result = json(cli.run(List.of("smapi", "get-skill-simulation",
                    "--skill-id", skillId, "--simulation-id", simulationId)));
            if (!"IN_PROGRESS".equals(result.path("status").asText())) {
                return parse(result);
            }
        }
        throw new IllegalStateException("El simulador de Alexa no respondió a tiempo.");
    }

    static Result parse(JsonNode simulation) {
        JsonNode result = simulation.path("result");
        if ("FAILED".equals(simulation.path("status").asText()) || result.has("error")) {
            String message = result.path("error").path("message").asText("Alexa no pudo procesar la frase.");
            return new Result(null, Map.of(), null, translateError(message));
        }
        JsonNode considered = result.path("alexaExecutionInfo").path("consideredIntents").path(0);
        String intent = considered.isMissingNode() ? null : considered.path("name").asText(null);
        Map<String, String> slots = new LinkedHashMap<>();
        considered.path("slots").properties().forEach(slot -> {
            String value = slot.getValue().path("value").asText(null);
            if (value != null) {
                slots.put(slot.getKey(), value);
            }
        });
        JsonNode invocation = result.path("skillExecutionInfo").path("invocations").path(0);
        JsonNode body = invocation.path("invocationResponse").path("body");
        String skillResponse = body.isObject() ? body.toString() : null;
        String caption = result.path("alexaExecutionInfo").path("alexaResponses").path(0)
                .path("content").path("caption").asText(null);
        return new Result(intent, slots, skillResponse, caption);
    }

    private static String translateError(String message) {
        if (message.contains("did not resolve to any intent")) {
            return "Alexa no relacionó esa frase con la skill (no hay una sesión abierta o la frase no coincide con ningún intent).";
        }
        return message;
    }

    private static JsonNode json(String text) {
        try {
            return JSON.readTree(text);
        } catch (IOException e) {
            throw new IllegalStateException("Respuesta inesperada de la ASK CLI: " + text, e);
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    /** The real ASK CLI ("ask" on the PATH, Homebrew folders added). */
    public static Cli askCli(String command) {
        return arguments -> {
            List<String> full = new ArrayList<>();
            full.add(command);
            full.addAll(arguments);
            ProcessBuilder builder = new ProcessBuilder(full).redirectErrorStream(false);
            builder.environment().merge("PATH", "/opt/homebrew/bin:/usr/local/bin", (a, b) -> a + ":" + b);
            try {
                Process process = builder.start();
                String out = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                String err = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
                if (process.waitFor() != 0) {
                    throw new IllegalStateException("La ASK CLI falló: " + (err.isBlank() ? out : err).trim()
                            + ". ¿Ejecutaste «ask configure»?");
                }
                return out;
            } catch (IOException e) {
                throw new IllegalStateException("No encuentro la ASK CLI («" + command + "»). Instálala con: npm install -g ask-cli", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        };
    }

}
