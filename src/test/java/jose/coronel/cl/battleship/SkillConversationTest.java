package jose.coronel.cl.battleship;

import com.amazon.ask.Skill;
import com.amazon.ask.model.Context;
import com.amazon.ask.model.Intent;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.LaunchRequest;
import com.amazon.ask.model.Request;
import com.amazon.ask.model.RequestEnvelope;
import com.amazon.ask.model.ResponseEnvelope;
import com.amazon.ask.model.Session;
import com.amazon.ask.model.Slot;
import com.amazon.ask.model.ui.SsmlOutputSpeech;
import jose.coronel.cl.battleship.handlers.ev3.directives.SendCommandDirective;
import jose.coronel.cl.battleship.robot.LoggingRobotChannel;
import jose.coronel.cl.battleship.robot.RobotChannels;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Plays the first turns of a game through the real skill (Alexa SDK request → response),
 * in Spanish and English, and checks what the robot receives.
 */
class SkillConversationTest {

    private final Skill skill = BatallaNavalAlexaSkill.getSkill();
    private LoggingRobotChannel robot;
    private Map<String, Object> session;

    @BeforeEach
    void setUp() {
        robot = new LoggingRobotChannel();
        RobotChannels.set(robot);
        session = new HashMap<>();
    }

    @Test
    void spanishGameStartsAndPlayerAttacks() {
        String welcome = say("es-ES", LaunchRequest.builder());
        assertTrue(welcome.contains("Batalla naval"), welcome);

        String start = say("es-ES", intent("ComenzarJuegoIntent", Map.of()));
        assertTrue(start.contains("modo de juego avanzado"), start);

        String mode = say("es-ES", intent("ModoDeJuegoIntent", Map.of("nivel", "avanzado")));
        assertTrue(mode.contains("Modo de juego avanzado configurado"), mode);

        String attack = say("es-ES", intent("AtaqueJugadorIntent", Map.of("Fila", "C", "Columna", "cinco")));
        assertTrue(attack.contains("Ejecutando ataque"), attack);
        assertTrue(attack.contains("Es mi turno de atacar"), attack);

        // Alexa's own shot is kept in the session so "impacto" / "al agua" can be recorded later
        assertTrue(session.containsKey("alexaAttackRow"));

        String fail = say("es-ES", intent("AlAguaAlexaIntent", Map.of()));
        assertTrue(fail.contains("Es tu turno de atacar"), fail);

        List<String> commands = robot.getSent().stream().map(SendCommandDirective::getCommand).toList();
        assertEquals(List.of("start_game", "game_mode_configured", "player_attack", "fail_attack"), commands);

        SendCommandDirective playerAttack = robot.getSent().get(2);
        assertEquals("2", playerAttack.getRow());     // C → row index 2
        assertEquals("4", playerAttack.getColumn());  // 5 → column index 4
    }

    @Test
    void englishGameStarts() {
        String start = say("en-US", intent("StartGameIntent", Map.of()));
        assertFalse(start.isBlank());
        String mode = say("en-US", intent("GameModeIntent", Map.of("level", "medium")));
        assertTrue(mode.contains("Game mode medium configured"), mode);
        assertEquals("start_game", robot.getSent().get(0).getCommand());
    }

    // --- helpers ---

    private static IntentRequest.Builder intent(String name, Map<String, String> slotValues) {
        Map<String, Slot> slots = new HashMap<>();
        slotValues.forEach((slot, value) -> slots.put(slot, Slot.builder().withName(slot).withValue(value).build()));
        return IntentRequest.builder().withIntent(Intent.builder().withName(name).withSlots(slots).build());
    }

    private String say(String locale, Object requestBuilder) {
        Request request;
        if (requestBuilder instanceof IntentRequest.Builder b) {
            request = b.withRequestId("req").withLocale(locale).withTimestamp(OffsetDateTime.now()).build();
        } else {
            request = ((LaunchRequest.Builder) requestBuilder)
                    .withRequestId("req").withLocale(locale).withTimestamp(OffsetDateTime.now()).build();
        }
        RequestEnvelope envelope = RequestEnvelope.builder()
                .withVersion("1.0")
                .withSession(Session.builder().withSessionId("session").withNew(false).withAttributes(session).build())
                .withContext(Context.builder().build())
                .withRequest(request)
                .build();

        ResponseEnvelope response = skill.invoke(envelope);
        if (response.getSessionAttributes() != null) {
            session = new HashMap<>(response.getSessionAttributes());
        }
        return ((SsmlOutputSpeech) response.getResponse().getOutputSpeech()).getSsml();
    }

}
