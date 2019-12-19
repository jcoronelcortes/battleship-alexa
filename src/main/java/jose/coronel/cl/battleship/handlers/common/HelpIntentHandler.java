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

public class HelpIntentHandler implements RequestHandler {

    private static Logger log = getLogger(HelpIntentHandler.class);

    @Override
    public boolean canHandle(HandlerInput input) {
        return input.matches(intentName("AMAZON.HelpIntent"));
    }

    @Override
    public Optional<Response> handle(HandlerInput input) {
        log.info("Executing HandlerInput");
        Locale locale = getLocale(input);
        return input.getResponseBuilder()
                .withSpeech(i18n(locale, "helpSpeechText"))
                .withReprompt(i18n(locale, "helpSpeechText"))
                .withShouldEndSession(false)
                .build();
    }
}