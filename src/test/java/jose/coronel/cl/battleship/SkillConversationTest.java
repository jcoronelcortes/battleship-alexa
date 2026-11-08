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
import com.amazon.ask.model.slu.entityresolution.Resolution;
import com.amazon.ask.model.slu.entityresolution.Resolutions;
import com.amazon.ask.model.slu.entityresolution.Status;
import com.amazon.ask.model.slu.entityresolution.StatusCode;
import com.amazon.ask.model.slu.entityresolution.Value;
import com.amazon.ask.model.slu.entityresolution.ValueWrapper;
import com.amazon.ask.model.ui.SsmlOutputSpeech;
import jose.coronel.cl.battleship.game.board.Game;
import jose.coronel.cl.battleship.game.board.Zone;
import jose.coronel.cl.battleship.session.GameSession;
import jose.coronel.cl.battleship.robot.RobotCommand;
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

        List<String> commands = robot.getSent().stream().map(RobotCommand::getCommand).toList();
        assertEquals(List.of("start_game", "game_mode_configured", "player_attack", "fail_attack"), commands);

        RobotCommand playerAttack = robot.getSent().get(2);
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

    // ---------- game flow guards (before the refactor these crashed or counted twice) ----------

    @Test
    void attackBeforeStartingAsksToStart() {
        String attack = say("es-ES", intent("AtaqueJugadorIntent", Map.of("Fila", "C", "Columna", "cinco")));
        assertTrue(attack.contains("Todavía no hemos comenzado una partida"), attack);
        assertTrue(robot.getSent().isEmpty());
    }

    @Test
    void answerBeforeAlexaShootsIsRejected() {
        say("es-ES", intent("ComenzarJuegoIntent", Map.of()));
        String hit = say("es-ES", intent("ImpactoAlexaIntent", Map.of()));
        assertTrue(hit.contains("No tengo ningún disparo esperando tu respuesta"), hit);
    }

    @Test
    void repeatedImpactCountsOnlyOnce() {
        say("es-ES", intent("ComenzarJuegoIntent", Map.of()));
        say("es-ES", intent("AtaqueJugadorIntent", Map.of("Fila", "A", "Columna", "uno")));
        say("es-ES", intent("ImpactoAlexaIntent", Map.of()));
        String again = say("es-ES", intent("ImpactoAlexaIntent", Map.of()));
        assertTrue(again.contains("No tengo ningún disparo esperando tu respuesta"), again);
        Game game = GameSession.fromJson((String) session.get("jsonGame"));
        assertEquals(1, game.getBoardGamePlayer().getPoints());
    }

    @Test
    void attackingBeforeAnsweringAlexaAsksForTheAnswer() {
        say("es-ES", intent("ComenzarJuegoIntent", Map.of()));
        say("es-ES", intent("AtaqueJugadorIntent", Map.of("Fila", "A", "Columna", "uno")));
        String row = (String) session.get("alexaAttackRow");
        String second = say("es-ES", intent("AtaqueJugadorIntent", Map.of("Fila", "B", "Columna", "dos")));
        assertTrue(second.contains("Primero dime cómo fue mi disparo en " + (char) ('A' + Integer.parseInt(row))), second);
        assertEquals(1, robot.getSent().stream().filter(c -> c.getCommand().equals("player_attack")).count());
    }

    @Test
    void unknownCellIsExplained() {
        say("es-ES", intent("ComenzarJuegoIntent", Map.of()));
        String attack = say("es-ES", intent("AtaqueJugadorIntent", Map.of("Fila", "C", "Columna", "diez")));
        assertTrue(attack.contains("No entendí la casilla"), attack);
    }

    @Test
    void corruptedGameInSessionIsTreatedAsNoGame() {
        session.put("jsonGame", "{not json");
        String attack = say("es-ES", intent("AtaqueJugadorIntent", Map.of("Fila", "C", "Columna", "cinco")));
        assertTrue(attack.contains("Todavía no hemos comenzado una partida"), attack);
    }

    @Test
    void unexpectedErrorIsAnsweredInThePlayersLanguage() {
        // Before the refactor this answer was always in English
        RobotChannels.set(command -> {
            throw new IllegalStateException("simulated failure");
        });
        String start = say("es-ES", intent("ComenzarJuegoIntent", Map.of()));
        assertTrue(start.contains("Lo siento, tuve un problema"), start);
    }

    @Test
    void spanishLevelSynonymUsesCanonicalValue() {
        say("es-ES", intent("ComenzarJuegoIntent", Map.of()));
        String mode = say("es-ES", resolvedIntent("ModoDeJuegoIntent", "nivel", "fácil", "básico"));
        assertTrue(mode.contains("Modo de juego básico configurado"), mode);
        Game game = GameSession.fromJson((String) session.get("jsonGame"));
        assertEquals(Game.BASIC_MODE, game.getGameMode());
    }

    @Test
    void sunkWithoutShipNameIsGuessed() {
        say("es-ES", intent("ComenzarJuegoIntent", Map.of()));
        say("es-ES", intent("ModoDeJuegoIntent", Map.of("nivel", "medio")));
        say("es-ES", intent("AtaqueJugadorIntent", Map.of("Fila", "A", "Columna", "uno")));
        String sunk = say("es-ES", intent("DestruidoIntent", Map.of()));
        assertTrue(sunk.contains("Solamente me queda hundir cuatro barcos"), sunk);
    }

    @Test
    void playerHearsSunkShipNameInSpanish() {
        say("es-ES", intent("ComenzarJuegoIntent", Map.of()));
        say("es-ES", intent("ModoDeJuegoIntent", Map.of("nivel", "básico")));

        // Find Alexa's destroyer (length 2) and shoot both cells
        Game game = GameSession.fromJson((String) session.get("jsonGame"));
        Zone[][] zones = game.getBoardGameAlexa().getZones();
        String last = "";
        for (int row = 0; row < zones.length; row++) {
            for (int column = 0; column < zones[row].length; column++) {
                if (zones[row][column].isHasShip() && zones[row][column].getShipLength() == 2) {
                    last = say("es-ES", intent("AtaqueJugadorIntent",
                            Map.of("Fila", String.valueOf((char) ('A' + row)), "Columna", String.valueOf(column + 1))));
                    say("es-ES", intent("AlAguaAlexaIntent", Map.of()));
                }
            }
        }
        assertTrue(last.contains("Tú has hundido un destructor"), last);
    }

    // --- helpers ---

    private static IntentRequest.Builder resolvedIntent(String name, String slotName, String spoken, String canonical) {
        Slot slot = Slot.builder().withName(slotName).withValue(spoken)
                .withResolutions(Resolutions.builder().addResolutionsPerAuthorityItem(Resolution.builder()
                        .withAuthority("amzn1.er-authority.echo-sdk.test")
                        .withStatus(Status.builder().withCode(StatusCode.ER_SUCCESS_MATCH).build())
                        .addValuesItem(ValueWrapper.builder().withValue(Value.builder().withName(canonical).build()).build())
                        .build()).build())
                .build();
        return IntentRequest.builder().withIntent(Intent.builder().withName(name).withSlots(Map.of(slotName, slot)).build());
    }

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
