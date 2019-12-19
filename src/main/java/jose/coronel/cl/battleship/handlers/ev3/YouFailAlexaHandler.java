package jose.coronel.cl.battleship.handlers.ev3;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.Response;
import jose.coronel.cl.battleship.game.BattleshipGame;
import jose.coronel.cl.battleship.game.board.Game;
import jose.coronel.cl.battleship.handlers.common.BattleshipHandler;
import jose.coronel.cl.battleship.handlers.ev3.directives.SendCommandDirective;
import jose.coronel.cl.battleship.util.MindstormAlexaUtil;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.Random;

import static com.amazon.ask.request.Predicates.intentName;
import static com.amazon.ask.request.Predicates.requestType;
import static jose.coronel.cl.battleship.util.I18NUtil.i18n;
import static org.slf4j.LoggerFactory.getLogger;

public class YouFailAlexaHandler extends BattleshipHandler {

    private static Logger log = getLogger(YouFailAlexaHandler.class);

    @Override
    public boolean validarHandle(HandlerInput handlerInput, IntentRequest intentRequest) {
        return handlerInput.matches(requestType(IntentRequest.class).and(intentName(i18n(locale, "youFailAlexaIntent"))));
    }

    @Override
    public Optional<Response> procesarHandle(HandlerInput handlerInput, IntentRequest intentRequest) {
        log.debug("Executing YouFailAlexaHandler");
        String alexaAtaqueFila = (String) MindstormAlexaUtil.getValueFromSession(handlerInput,"alexaAttackRow");
        String alexaAtaqueColumna = (String) MindstormAlexaUtil.getValueFromSession(handlerInput,"alexaAttackColumn");
        log.info("Impact attack : [{}, {}]", alexaAtaqueFila, alexaAtaqueColumna);

        String jsonGame = (String) MindstormAlexaUtil.getValueFromSession(handlerInput, "jsonGame");
        log.debug("Get game status from session");

        Game game = MindstormAlexaUtil.jsonToGame(jsonGame);
        log.debug("Game restored from JSON information");

        new BattleshipGame().saveFailImpactOnPlayerBoard(game, alexaAtaqueFila, alexaAtaqueColumna);
        log.info("Player board updated with fail impact");

        jsonGame = MindstormAlexaUtil.gameToJson(game);
        log.debug("New JSON information with game status created");

        MindstormAlexaUtil.putSessionAttribute(handlerInput, "jsonGame", jsonGame);
        log.debug("New game status saved on session with key 'jsonGame'");

        SendCommandDirective payload = new SendCommandDirective("command","fail_attack");
        int randomSpeech = new Random().nextInt(3) + 1;
        return handlerInput.getResponseBuilder()
                .withSpeech(i18n(locale, "itIsYourTurnSpeechText" + randomSpeech))
                .withReprompt(i18n(locale, "reprompt"))
                .withShouldEndSession(false)
                .addDirective(MindstormAlexaUtil.buildDirective(endpointId, MindstormAlexaUtil.NAMESPACE, MindstormAlexaUtil.NAME_CONTROL, payload))
                .build();
    }
}
