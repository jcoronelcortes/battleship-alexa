package jose.coronel.cl.battleship.handlers.common;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.impl.IntentRequestHandler;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.Response;
import jose.coronel.cl.battleship.handlers.ev3.directives.SendCommandDirective;
import jose.coronel.cl.battleship.robot.RobotChannels;

import java.util.Locale;
import java.util.Optional;

import static jose.coronel.cl.battleship.util.I18NUtil.getLocale;

public abstract class BattleshipHandler implements IntentRequestHandler {

    protected Locale locale;

    @Override
    public boolean canHandle(HandlerInput handlerInput, IntentRequest intentRequest) {
        locale = getLocale(handlerInput);
        return validarHandle(handlerInput, intentRequest);
    }

    @Override
    public Optional<Response> handle(HandlerInput handlerInput, IntentRequest intentRequest) {
        locale = getLocale(handlerInput);
        return procesarHandle(handlerInput, intentRequest);
    }

    /** Sends a command to the EV3 robot (AWS IoT Core, or dummy mode when not configured). */
    protected void sendToRobot(SendCommandDirective payload) {
        RobotChannels.get().send(payload);
    }


    public abstract boolean validarHandle(HandlerInput handlerInput, IntentRequest intentRequest);

    public abstract Optional<Response> procesarHandle(HandlerInput handlerInput, IntentRequest intentRequest);

}
