package jose.coronel.cl.battleship.handlers.common;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.LaunchRequest;
import com.amazon.ask.model.Response;
import com.amazon.ask.request.Predicates;
import org.slf4j.Logger;

import java.util.Locale;
import java.util.Optional;

import static jose.coronel.cl.battleship.util.I18NUtil.getLocale;
import static jose.coronel.cl.battleship.util.I18NUtil.i18n;
import static org.slf4j.LoggerFactory.getLogger;

public class LaunchRequestHandler implements RequestHandler {

    private static Logger log = getLogger(LaunchRequestHandler.class);

    @Override
    public boolean canHandle(HandlerInput input) {
        return input.matches(Predicates.requestType(LaunchRequest.class));
    }

    @Override
    public Optional<Response> handle(HandlerInput input) {
        log.info("Executing LaunchRequestHandler");
        Locale locale = getLocale(input);
        // The robot is reached through AWS IoT Core, so there is no Echo-paired gadget to look up
        return input.getResponseBuilder()
                .withSpeech(i18n(locale, "welcome"))
                .withReprompt(i18n(locale, "reprompt"))
                .withShouldEndSession(false)
                .build();
    }

}
