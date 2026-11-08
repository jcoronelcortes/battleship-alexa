package jose.coronel.cl.battleship.handlers.exception;

import com.amazon.ask.dispatcher.exception.ExceptionHandler;
import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.Response;
import org.slf4j.Logger;

import java.util.Locale;
import java.util.Optional;

import static jose.coronel.cl.battleship.util.I18NUtil.getLocale;
import static jose.coronel.cl.battleship.util.I18NUtil.i18n;
import static org.slf4j.LoggerFactory.getLogger;

/** Last resort: logs the error and asks the player to repeat, in their language. The game stays open. */
public class GenericExceptionHandler implements ExceptionHandler {

    private static final Logger log = getLogger(GenericExceptionHandler.class);

    @Override
    public boolean canHandle(HandlerInput input, Throwable throwable) {
        return true;
    }

    @Override
    public Optional<Response> handle(HandlerInput input, Throwable throwable) {
        log.error("Unexpected error: {}", throwable.getMessage(), throwable);
        Locale locale = getLocale(input);
        String speech = i18n(locale, "errorSpeechText");
        return input.getResponseBuilder()
                .withSpeech(speech)
                .withReprompt(speech)
                .withShouldEndSession(false)
                .build();
    }

}
