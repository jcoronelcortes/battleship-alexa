package jose.coronel.cl.battleship.handlers.common;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.impl.IntentRequestHandler;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.Response;
import jose.coronel.cl.battleship.util.MindstormAlexaUtil;

import java.util.Locale;
import java.util.Optional;

import static jose.coronel.cl.battleship.util.I18NUtil.getLocale;

public abstract class BattleshipHandler implements IntentRequestHandler {

    protected Locale locale;
    protected String endpointId;

    @Override
    public boolean canHandle(HandlerInput handlerInput, IntentRequest intentRequest) {
        locale = getLocale(handlerInput);
        return validarHandle(handlerInput, intentRequest);
    }

    @Override
    public Optional<Response> handle(HandlerInput handlerInput, IntentRequest intentRequest) {
        locale = getLocale(handlerInput);
        endpointId = (String) MindstormAlexaUtil.getValueFromSession(handlerInput, "endpointId");
        if (endpointId == null) {
            // endpointId is optional, use default if not available
            endpointId = "";
        }
        return procesarHandle(handlerInput, intentRequest);
    }


    public abstract boolean validarHandle(HandlerInput handlerInput, IntentRequest intentRequest);

    public abstract Optional<Response> procesarHandle(HandlerInput handlerInput, IntentRequest intentRequest);

}
