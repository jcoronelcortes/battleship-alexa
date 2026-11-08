package jose.coronel.cl.battleship.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.Response;
import com.amazon.ask.model.Slot;
import com.amazon.ask.model.slu.entityresolution.Resolution;
import com.amazon.ask.model.slu.entityresolution.StatusCode;
import jose.coronel.cl.battleship.robot.RobotChannels;
import jose.coronel.cl.battleship.robot.RobotCommand;
import jose.coronel.cl.battleship.session.GameSession;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import static jose.coronel.cl.battleship.util.I18NUtil.getLocale;
import static jose.coronel.cl.battleship.util.I18NUtil.i18n;

/** Everything a handler needs for one player turn: language, texts, slots, session, robot and the reply. */
public class Turn {

    private static final Random RANDOM = new Random();

    private final HandlerInput input;
    private final IntentRequest request;
    private final Locale locale;
    private final GameSession session;

    public Turn(HandlerInput input, IntentRequest request) {
        this.input = input;
        this.request = request;
        this.locale = getLocale(input);
        this.session = new GameSession(input.getAttributesManager());
    }

    public GameSession session() {
        return session;
    }

    public Locale locale() {
        return locale;
    }

    /** Translated text from translations_xx.properties. */
    public String text(String key, String... arguments) {
        return i18n(locale, key, arguments);
    }

    /** One of keyPrefix1 … keyPrefixN, at random (so Alexa does not always say the same). */
    public String randomText(String keyPrefix, int variants) {
        return text(keyPrefix + (RANDOM.nextInt(variants) + 1));
    }

    /**
     * Value of a slot whose name is a translation key (Spanish and English slot names differ).
     * Returns Alexa's canonical value ("fácil" → "básico"), the spoken value if there is no match,
     * or null if the slot is empty.
     */
    public String slot(String slotNameKey) {
        Map<String, Slot> slots = request.getIntent().getSlots();
        Slot slot = slots == null ? null : slots.get(text(slotNameKey));
        if (slot == null || slot.getValue() == null || slot.getValue().isBlank()) {
            return null;
        }
        if (slot.getResolutions() != null && slot.getResolutions().getResolutionsPerAuthority() != null) {
            for (Resolution resolution : slot.getResolutions().getResolutionsPerAuthority()) {
                if (resolution.getStatus() != null
                        && resolution.getStatus().getCode() == StatusCode.ER_SUCCESS_MATCH
                        && resolution.getValues() != null && !resolution.getValues().isEmpty()) {
                    return resolution.getValues().get(0).getValue().getName();
                }
            }
        }
        return slot.getValue();
    }

    public void sendToRobot(RobotCommand command) {
        RobotChannels.get().send(command);
    }

    /** Speak and keep the game open, waiting for the player. */
    public Optional<Response> ask(String speech) {
        return input.getResponseBuilder()
                .withSpeech(speech)
                .withReprompt(text("reprompt"))
                .withShouldEndSession(false)
                .build();
    }

    /** Speak and end the session (game over). */
    public Optional<Response> tell(String speech) {
        return input.getResponseBuilder()
                .withSpeech(speech)
                .withShouldEndSession(true)
                .build();
    }

}
