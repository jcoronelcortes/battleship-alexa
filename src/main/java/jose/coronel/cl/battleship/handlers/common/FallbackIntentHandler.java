package jose.coronel.cl.battleship.handlers.common;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.Response;

import java.util.Optional;
import org.slf4j.Logger;
import java.util.Locale;

import static com.amazon.ask.request.Predicates.intentName;
import static jose.coronel.cl.battleship.util.I18NUtil.getLocale;
import static jose.coronel.cl.battleship.util.I18NUtil.i18n;
import static org.slf4j.LoggerFactory.getLogger;

public class FallbackIntentHandler implements RequestHandler {

    private static Logger log = getLogger(FallbackIntentHandler.class);

    @Override
    public boolean canHandle(HandlerInput input) {
        return input.matches(intentName("AMAZON.FallbackIntent"));
    }

    @Override
    public Optional<Response> handle(HandlerInput input) {
        log.info("Executing FallbackIntentHandler");
        Locale locale = getLocale(input);
        return input.getResponseBuilder()
                .withSpeech(i18n(locale, "fallbackSpeechText"))
                .withReprompt(i18n(locale, "fallbackSpeechText"))
                .withShouldEndSession(false)
                .build();
    }
}

