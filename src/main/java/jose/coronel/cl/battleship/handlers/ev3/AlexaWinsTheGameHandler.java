package jose.coronel.cl.battleship.handlers.ev3;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.Response;
import jose.coronel.cl.battleship.handlers.common.BattleshipHandler;
import jose.coronel.cl.battleship.handlers.ev3.directives.SendCommandDirective;
import jose.coronel.cl.battleship.util.MindstormAlexaUtil;
import org.slf4j.Logger;

import java.util.Optional;

import static com.amazon.ask.request.Predicates.intentName;
import static com.amazon.ask.request.Predicates.requestType;
import static jose.coronel.cl.battleship.util.I18NUtil.i18n;
import static org.slf4j.LoggerFactory.getLogger;

public class AlexaWinsTheGameHandler extends BattleshipHandler {

    private static Logger log = getLogger(AlexaWinsTheGameHandler.class);

    @Override
    public boolean validarHandle(HandlerInput handlerInput, IntentRequest intentRequest) {
        return handlerInput.matches(requestType(IntentRequest.class).and(intentName(i18n(locale, "alexaWinsTheGameIntent"))));
    }

    @Override
    public Optional<Response> procesarHandle(HandlerInput handlerInput, IntentRequest intentRequest) {
        log.debug("Executing AlexaWinsTheGameHandler");
        SendCommandDirective payload = new SendCommandDirective("command","finish_game");
        return handlerInput.getResponseBuilder()
                .withSpeech(i18n(locale, "alexaWinsSpeechText"))
                .addDirective(MindstormAlexaUtil.buildDirective(endpointId, MindstormAlexaUtil.NAMESPACE, MindstormAlexaUtil.NAME_CONTROL, payload))
                .withShouldEndSession(true)
                .build();
    }
}
