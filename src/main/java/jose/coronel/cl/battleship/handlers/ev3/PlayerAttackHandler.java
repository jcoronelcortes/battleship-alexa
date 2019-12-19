package jose.coronel.cl.battleship.handlers.ev3;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.Response;
import jose.coronel.cl.battleship.handlers.common.BattleshipHandler;
import jose.coronel.cl.battleship.handlers.ev3.directives.SendCommandDirective;
import jose.coronel.cl.battleship.game.BattleshipGame;
import jose.coronel.cl.battleship.game.board.Game;
import jose.coronel.cl.battleship.util.PositionConvertion;
import jose.coronel.cl.battleship.util.MindstormAlexaUtil;
import jose.coronel.cl.battleship.util.AttackBoardTime;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.Random;

import static com.amazon.ask.request.Predicates.intentName;
import static com.amazon.ask.request.Predicates.requestType;
import static jose.coronel.cl.battleship.util.I18NUtil.i18n;
import static org.slf4j.LoggerFactory.getLogger;

public class PlayerAttackHandler extends BattleshipHandler {

    private static Logger log = getLogger(PlayerAttackHandler.class);

    @Override
    public boolean validarHandle(HandlerInput handlerInput, IntentRequest intentRequest) {
        return handlerInput.matches(requestType(IntentRequest.class).and(intentName(i18n(locale, "playerAttackIntent"))));
    }

    @Override
    public Optional<Response> procesarHandle(HandlerInput handlerInput, IntentRequest intentRequest) {
        log.debug("Executing PlayerAttackHandler");

        String columnRequest = MindstormAlexaUtil.getSlotFromIntentRequest(intentRequest, i18n(locale, "column")).toUpperCase();

        String rowRequest = MindstormAlexaUtil.getSlotFromIntentRequest(intentRequest, i18n(locale, "row")).toUpperCase();
        if(rowRequest.length() > 1){
            rowRequest = String.valueOf(rowRequest.charAt(0));
        }

        int column = PositionConvertion.convertTextToNumber(columnRequest);
        log.info("Attack [{}, {}]", rowRequest, column);

        String jsonGame = (String) MindstormAlexaUtil.getValueFromSession(handlerInput, "jsonGame");
        log.debug("Get game status from session");

        Game game = MindstormAlexaUtil.jsonToGame(jsonGame);
        log.debug("Game status restored form JSON");

        log.debug("Execute player attack");
        SendCommandDirective payload = new BattleshipGame().executePlayerAttack(game, rowRequest, column);

        jsonGame = MindstormAlexaUtil.gameToJson(game);
        log.debug("New game status created from JSON information");

        MindstormAlexaUtil.putSessionAttribute(handlerInput, "jsonGame", jsonGame);
        log.debug("New game status saved on the session with key 'jsonGame'");

        int attackResult = payload.getAttackResult();
        if(attackResult == 4){
            log.info("Player won the game");
            payload = new SendCommandDirective("command","finish_game");
            return handlerInput.getResponseBuilder()
                    .withSpeech(i18n(locale, "impactYouWin"))
                    .addDirective(MindstormAlexaUtil.buildDirective(endpointId, MindstormAlexaUtil.NAMESPACE, MindstormAlexaUtil.NAME_CONTROL, payload))
                    .withShouldEndSession(true)
                    .build();
        }
        else{
            log.info("Inform result of the attack to the player");
            String response = buildAttackResponse(payload, PositionConvertion.convertRowLetterToNumber(rowRequest), column);
            log.error("Response attack : {}", response);
            return handlerInput.getResponseBuilder()
                    .withSpeech(response)
                    .withReprompt(i18n(locale, "reprompt"))
                    .withShouldEndSession(false)
                    .addDirective(MindstormAlexaUtil.buildDirective(endpointId, MindstormAlexaUtil.NAMESPACE, MindstormAlexaUtil.NAME_CONTROL, payload))
                    .build();
        }
    }


    private String buildAttackResponse(SendCommandDirective payload, int row, int column){
        int randomSpeech;
        switch (payload.getAttackResult())  {
            case 1:
                randomSpeech = new Random().nextInt(4) + 1;
                return i18n(locale, "impactAttack" + randomSpeech,
                        AttackBoardTime.getSsmlBreaksOnMillisecondsBeforeToAttack(row, column - 1),
                        "<audio src=\"soundbank://soundlibrary/guns/cannons/cannons_02\"/>",
                        AttackBoardTime.getSsmlBreaksOnMillisecondsAfterToAttack(row, column - 1));
            case 2:
                return i18n(locale, "impactDestroyed",
                        AttackBoardTime.getSsmlBreaksOnMillisecondsBeforeToAttack(row, column - 1),
                        "<audio src=\"soundbank://soundlibrary/guns/cannons/cannons_02\"/>",
                        payload.getShipDestroyed(),
                        AttackBoardTime.getSsmlBreaksOnMillisecondsAfterToAttack(row, column - 1));
            case 3:
                randomSpeech = new Random().nextInt(4) + 1;
                return i18n(locale, "impactFail" + randomSpeech,
                        AttackBoardTime.getSsmlBreaksOnMillisecondsBeforeToAttack(row, column - 1),
                        "<audio src=\"soundbank://soundlibrary/guns/cannons/cannons_02\"/>",
                        AttackBoardTime.getSsmlBreaksOnMillisecondsAfterToAttack(row, column - 1));
            case 4:
                return i18n(locale, "impactYouWin",
                        "<audio src=\"soundbank://soundlibrary/gameshow/gameshow_01\"/>");
            case 5:
                return i18n(locale, "zoneAlreadyAttacked");
            default:
                return "";
        }
    }


}
