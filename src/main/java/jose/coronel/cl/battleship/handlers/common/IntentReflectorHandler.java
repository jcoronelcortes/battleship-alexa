package jose.coronel.cl.battleship.handlers.common;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.impl.IntentRequestHandler;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.Response;

import java.util.Optional;
import org.slf4j.Logger;
import java.util.Locale;

import static jose.coronel.cl.battleship.util.I18NUtil.getLocale;
import static jose.coronel.cl.battleship.util.I18NUtil.i18n;
import static org.slf4j.LoggerFactory.getLogger;

public class IntentReflectorHandler implements IntentRequestHandler {

    private static Logger log = getLogger(IntentReflectorHandler.class);

    @Override
    public boolean canHandle(HandlerInput handlerInput, IntentRequest intentRequest) {
        return true;
    }

    @Override
    public Optional<Response> handle(HandlerInput handlerInput, IntentRequest intentRequest) {
        log.info("Executing IntentReflectorHandler");
        Locale locale = getLocale(handlerInput);
        String intentName = intentRequest.getIntent().getName();
        String speakOutput = i18n(locale, "reflectorSpeechText", intentName);
        return handlerInput.getResponseBuilder()
                .withSpeech(speakOutput)
                .withReprompt(i18n(locale, "reflectorError"))
                .withShouldEndSession(false)
                .build();
    }

}
