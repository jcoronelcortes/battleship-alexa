package jose.coronel.cl.battleship;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Checks that the Alexa interaction models (alexa-skill/*.json) match what the Java handlers expect.
 * In 2020 the handlers for game mode and destroyed existed but the model did not have those intents.
 */
class InteractionModelTest {

    /** Translation key of each intent name → translation key of the slot it reads (or "-" for none). */
    private static final Map<String, String> INTENT_SLOTS = Map.of(
            "startGameIntent", "-",
            "playerAttackIntent", "row,column",
            "youFailAlexaIntent", "-",
            "impactAlexaIntent", "-",
            "alexaWinsTheGameIntent", "-",
            "testModeIntent", "-",
            "destroyedIntent", "ship",
            "gameModeIntent", "gameMode");

    @ParameterizedTest
    @CsvSource({"alexa-skill/Battleship_es.json, es", "alexa-skill/Battleship_en.json, en"})
    void modelMatchesHandlers(String modelFile, String language) throws Exception {
        JsonNode model = new ObjectMapper().readTree(new File(modelFile)).path("interactionModel").path("languageModel");
        ResourceBundle texts = ResourceBundle.getBundle("i18n.translations", Locale.forLanguageTag(language));

        Map<String, JsonNode> intents = new HashMap<>();
        model.path("intents").forEach(intent -> intents.put(intent.path("name").asText(), intent));
        Set<String> types = new HashSet<>();
        model.path("types").forEach(type -> types.add(type.path("name").asText()));

        for (Map.Entry<String, String> entry : INTENT_SLOTS.entrySet()) {
            String intentName = texts.getString(entry.getKey());
            JsonNode intent = intents.get(intentName);
            if (intent == null) {
                fail(modelFile + " is missing intent " + intentName);
            }
            assertTrue(intent.path("samples").size() > 0, intentName + " has no sample phrases");
            if (!entry.getValue().equals("-")) {
                for (String slotKey : entry.getValue().split(",")) {
                    String slotName = texts.getString(slotKey);
                    JsonNode slot = findSlot(intent, slotName);
                    if (slot == null) {
                        fail(intentName + " in " + modelFile + " is missing slot " + slotName);
                    }
                    String type = slot.path("type").asText();
                    assertTrue(type.startsWith("AMAZON.") || types.contains(type), "slot type " + type + " is not defined");
                }
            }
        }
    }

    @ParameterizedTest
    @CsvSource({"alexa-skill/Battleship_es.json", "alexa-skill/Battleship_en.json"})
    void samplePhrasesAreNotRepeatedAcrossIntents(String modelFile) throws Exception {
        JsonNode model = new ObjectMapper().readTree(new File(modelFile)).path("interactionModel").path("languageModel");
        Map<String, String> owner = new HashMap<>();
        model.path("intents").forEach(intent -> intent.path("samples").forEach(sample -> {
            String phrase = sample.asText().toLowerCase(Locale.ROOT);
            String previous = owner.put(phrase, intent.path("name").asText());
            if (previous != null) {
                fail("\"" + phrase + "\" is used by " + previous + " and " + intent.path("name").asText());
            }
        }));
        assertEquals(owner.size(), owner.keySet().size());
    }

    private static JsonNode findSlot(JsonNode intent, String name) {
        for (JsonNode slot : intent.path("slots")) {
            if (slot.path("name").asText().equals(name)) {
                return slot;
            }
        }
        return null;
    }

}
