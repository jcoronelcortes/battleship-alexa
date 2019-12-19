package jose.coronel.cl.battleship.handlers.ev3;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.Response;
import jose.coronel.cl.battleship.game.board.Game;
import jose.coronel.cl.battleship.handlers.common.BattleshipHandler;
import jose.coronel.cl.battleship.handlers.ev3.directives.SendCommandDirective;
import jose.coronel.cl.battleship.game.BattleshipGame;
import jose.coronel.cl.battleship.util.PositionConvertion;
import jose.coronel.cl.battleship.util.MindstormAlexaUtil;
import org.slf4j.Logger;

import java.util.Optional;

import static com.amazon.ask.request.Predicates.intentName;
import static com.amazon.ask.request.Predicates.requestType;
import static jose.coronel.cl.battleship.util.I18NUtil.i18n;
import static org.slf4j.LoggerFactory.getLogger;

public class AlexaAttackHandler extends BattleshipHandler {

    private static Logger log = getLogger(AlexaAttackHandler.class);

    @Override
    public boolean validarHandle(HandlerInput handlerInput, IntentRequest intentRequest) {
        return handlerInput.matches(requestType(IntentRequest.class).and(intentName(i18n(locale, "alexaAttackIntent"))));
    }

    @Override
    public Optional<Response> procesarHandle(HandlerInput handlerInput, IntentRequest intentRequest) {
        log.debug("Executing AlexaAttackHandler");

        String jsonGamne = (String) MindstormAlexaUtil.getValueFromSession(handlerInput, "jsonGame");
        log.debug("Get game status from the session");

        Game alexa = MindstormAlexaUtil.jsonToGame(jsonGamne);
        log.debug("Game status restored from JSON");

        int[] attack = new BattleshipGame().generateAlexaAttack(alexa);
        int row = attack[0];
        int column = attack[1];
        MindstormAlexaUtil.putSessionAttribute(handlerInput, "alexaAttackRow", String.valueOf(row));
        MindstormAlexaUtil.putSessionAttribute(handlerInput, "alexaAttackColumn", String.valueOf(column));

        String rowLetter = PositionConvertion.convertNumberToRowLetter(row);
        column = column + 1;
        log.info("Alexa has executed an attack on : [{}, {}]", rowLetter, column);

        SendCommandDirective payload = new SendCommandDirective("command","alexa_attack", rowLetter, String.valueOf(column));
        return handlerInput.getResponseBuilder()
                .withSpeech(i18n(locale, "alexaAttackSpeechText", rowLetter, String.valueOf(column)))
                .withReprompt(i18n(locale, "reprompt"))
                .withShouldEndSession(false)
                .addDirective(MindstormAlexaUtil.buildDirective(endpointId, MindstormAlexaUtil.NAMESPACE, MindstormAlexaUtil.NAME_CONTROL, payload))
                .build();
    }

}
