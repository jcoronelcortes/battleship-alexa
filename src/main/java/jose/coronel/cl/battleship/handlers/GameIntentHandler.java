package jose.coronel.cl.battleship.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.impl.IntentRequestHandler;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.Response;

import java.util.Optional;

import static jose.coronel.cl.battleship.util.I18NUtil.getLocale;
import static jose.coronel.cl.battleship.util.I18NUtil.i18n;

/**
 * Base for the game intents. Intent names differ per language ("ComenzarJuegoIntent" / "StartGameIntent"),
 * so each handler gives the translation key of its intent name.
 */
public abstract class GameIntentHandler implements IntentRequestHandler {

    /** Translation key of the intent name, e.g. "startGameIntent". */
    protected abstract String intentKey();

    protected abstract Optional<Response> handle(Turn turn);

    @Override
    public boolean canHandle(HandlerInput input, IntentRequest request) {
        return request.getIntent().getName().equals(i18n(getLocale(input), intentKey()));
    }

    @Override
    public Optional<Response> handle(HandlerInput input, IntentRequest request) {
        return handle(new Turn(input, request));
    }

}
