package jose.coronel.cl.battleship.handlers.ev3;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.Response;
import jose.coronel.cl.battleship.handlers.common.BattleshipHandler;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.amazon.ask.request.Predicates.intentName;
import static com.amazon.ask.request.Predicates.requestType;
import static jose.coronel.cl.battleship.util.I18NUtil.i18n;
import static org.slf4j.LoggerFactory.getLogger;

public class PausaHandler extends BattleshipHandler {

    private static Logger log = getLogger(PausaHandler.class);

    @Override
    public boolean validarHandle(HandlerInput handlerInput, IntentRequest intentRequest) {
        return handlerInput.matches(requestType(IntentRequest.class).and(intentName(i18n(locale, "pausaIntent"))));
    }

    @Override
    public Optional<Response> procesarHandle(HandlerInput handlerInput, IntentRequest intentRequest) {
        log.info("Executing PausaHandler");
        try {
            log.info("Start pausa");
            TimeUnit.SECONDS.sleep(5);
            log.info("End pausa");
        }
        catch (InterruptedException e) {
            log.error("Error message : {}", e.getMessage(), e);
        }
        return handlerInput.getResponseBuilder()
                .withSpeech(i18n(locale, "waitCompleted"))
                .withReprompt(i18n(locale, "reprompt"))
                .withShouldEndSession(false)
                .build();
    }

}
