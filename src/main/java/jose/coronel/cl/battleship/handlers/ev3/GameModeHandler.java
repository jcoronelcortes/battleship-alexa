package jose.coronel.cl.battleship.handlers.ev3;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.Response;
import jose.coronel.cl.battleship.game.board.Game;
import jose.coronel.cl.battleship.handlers.common.BattleshipHandler;
import jose.coronel.cl.battleship.handlers.ev3.directives.SendCommandDirective;
import jose.coronel.cl.battleship.util.GameModeUtil;
import jose.coronel.cl.battleship.util.MindstormAlexaUtil;
import org.slf4j.Logger;

import java.util.Optional;

import static com.amazon.ask.request.Predicates.intentName;
import static com.amazon.ask.request.Predicates.requestType;
import static jose.coronel.cl.battleship.util.I18NUtil.i18n;
import static org.slf4j.LoggerFactory.getLogger;

public class GameModeHandler extends BattleshipHandler {

    private static Logger log = getLogger(GameModeHandler.class);


    @Override
    public boolean validarHandle(HandlerInput handlerInput, IntentRequest intentRequest) {
        return handlerInput.matches(requestType(IntentRequest.class).and(intentName(i18n(locale, "gameModeIntent"))));
    }

    @Override
    public Optional<Response> procesarHandle(HandlerInput handlerInput, IntentRequest intentRequest) {
        log.debug("Executing GameModeHandler");
        String gameMode = MindstormAlexaUtil.getSlotFromIntentRequest(intentRequest, i18n(locale, "gameMode")).toUpperCase();
        log.info("Game mode : {}", gameMode);

        String jsonGame = (String) MindstormAlexaUtil.getValueFromSession(handlerInput, "jsonGame");
        log.debug("Get game status from session");

        Game game = MindstormAlexaUtil.jsonToGame(jsonGame);
        log.debug("Game status restored form JSON");

        game.setGameMode(GameModeUtil.getGameMode(gameMode));

        jsonGame = MindstormAlexaUtil.gameToJson(game);
        log.debug("New JSON information with game status created");

        MindstormAlexaUtil.putSessionAttribute(handlerInput, "jsonGame", jsonGame);
        log.debug("New game status saved on session with key 'jsonGame'");

        SendCommandDirective payload = new SendCommandDirective("command","game_mode_configured");
        sendToRobot(payload);
        return handlerInput.getResponseBuilder()
                .withSpeech(i18n(locale, "gameModeSpeechText", gameMode.toLowerCase()))
                .withReprompt(i18n(locale, "reprompt"))
                .withShouldEndSession(false)
                .build();
    }

}
