package jose.coronel.cl.battleship.handlers.ev3;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.Response;
import jose.coronel.cl.battleship.game.board.Attack;
import jose.coronel.cl.battleship.game.board.Game;
import jose.coronel.cl.battleship.handlers.common.BattleshipHandler;
import jose.coronel.cl.battleship.handlers.ev3.directives.SendCommandDirective;
import jose.coronel.cl.battleship.util.MindstormAlexaUtil;
import org.slf4j.Logger;

import java.util.List;
import java.util.Optional;
import java.util.Random;

import static com.amazon.ask.request.Predicates.intentName;
import static com.amazon.ask.request.Predicates.requestType;
import static jose.coronel.cl.battleship.util.I18NUtil.i18n;
import static org.slf4j.LoggerFactory.getLogger;

public class DestroyedHandler extends BattleshipHandler {

    private static Logger log = getLogger(DestroyedHandler.class);


    @Override
    public boolean validarHandle(HandlerInput handlerInput, IntentRequest intentRequest) {
        return handlerInput.matches(requestType(IntentRequest.class).and(intentName(i18n(locale, "destroyedIntent"))));
    }

    @Override
    public Optional<Response> procesarHandle(HandlerInput handlerInput, IntentRequest intentRequest) {
        log.debug("Executing DestroyedHandler");

        String shipType = MindstormAlexaUtil.getSlotFromIntentRequest(intentRequest, i18n(locale, "ship")).toUpperCase();
        log.info("Ship destroyed : {}", shipType);

        String jsonGamne = (String) MindstormAlexaUtil.getValueFromSession(handlerInput, "jsonGame");
        log.debug("Get game status from the session");

        Game game = MindstormAlexaUtil.jsonToGame(jsonGamne);
        log.debug("Game status restored from JSON");

        String alexaAttackRow = (String) MindstormAlexaUtil.getValueFromSession(handlerInput, "alexaAttackRow");
        String alexaAttackColumn = (String) MindstormAlexaUtil.getValueFromSession(handlerInput, "alexaAttackColumn");

        log.info("Update Zones to DESTROYED and clean impact list");
        int pendingShips = game.getBoardGamePlayer().updateZonesToDestroyedAndCleanImpacts(game, Integer.parseInt(alexaAttackRow), Integer.parseInt(alexaAttackColumn), shipType);
        log.info("Number of pending ships to destroy : {}", pendingShips);

        String jsonGame = MindstormAlexaUtil.gameToJson(game);
        log.debug("New game status created from JSON information");

        MindstormAlexaUtil.putSessionAttribute(handlerInput, "jsonGame", jsonGame);
        log.debug("New game status saved on the session with key 'jsonGame'");

        SendCommandDirective payload = new SendCommandDirective("command","alexa_ship_destroyed");
        return handlerInput.getResponseBuilder()
                .withSpeech(i18n(locale, "alexaDestroySpeechText", i18n(locale, "shipLengthDestroySpeechText" + pendingShips)))
                .withReprompt(i18n(locale, "reprompt"))
                .withShouldEndSession(false)
                .addDirective(MindstormAlexaUtil.buildDirective(endpointId, MindstormAlexaUtil.NAMESPACE, MindstormAlexaUtil.NAME_CONTROL, payload))
                .build();
    }

}
