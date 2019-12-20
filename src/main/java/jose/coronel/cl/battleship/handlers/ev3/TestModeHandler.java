package jose.coronel.cl.battleship.handlers.ev3;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.Response;
import jose.coronel.cl.battleship.game.board.Game;
import jose.coronel.cl.battleship.handlers.common.BattleshipHandler;
import jose.coronel.cl.battleship.handlers.ev3.directives.SendCommandDirective;
import jose.coronel.cl.battleship.util.MindstormAlexaUtil;
import jose.coronel.cl.battleship.util.PositionConvertion;
import org.slf4j.Logger;

import java.util.Optional;

import static com.amazon.ask.request.Predicates.intentName;
import static com.amazon.ask.request.Predicates.requestType;
import static jose.coronel.cl.battleship.util.I18NUtil.i18n;
import static org.slf4j.LoggerFactory.getLogger;

public class TestModeHandler extends BattleshipHandler {

    private static Logger log = getLogger(TestModeHandler.class);

    @Override
    public boolean validarHandle(HandlerInput handlerInput, IntentRequest intentRequest) {
        return handlerInput.matches(requestType(IntentRequest.class).and(intentName(i18n(locale, "testModeIntent"))));
    }

    @Override
    public Optional<Response> procesarHandle(HandlerInput handlerInput, IntentRequest intentRequest) {
        log.debug("Executing TestModeHandler");

        String jsonGamne = (String) MindstormAlexaUtil.getValueFromSession(handlerInput, "jsonGame");
        log.debug("Get game status from the session");

        Game alexa = MindstormAlexaUtil.jsonToGame(jsonGamne);
        log.debug("Game status restored from JSON");

        StringBuilder zones = new StringBuilder();
        StringBuilder logZones = new StringBuilder();
        for(int row=0; row <9; row++ ){
            for(int column=0; column <9; column++ ){
                if(alexa.getBoardGameAlexa().hasShip(row, column)){
                    String rowLetter = PositionConvertion.convertNumberToRowLetter(row);
                    int columnValue = column + 1;
                    zones.append("<p>").append(rowLetter).append("</p><break time=\"100ms\"/><p>").append(columnValue).append("</p><break time=\"100ms\"/>");
                    logZones.append("[ ").append(rowLetter).append(" , ").append(columnValue).append(" ] ");
                }
            }
        }

        SendCommandDirective payload = new SendCommandDirective("command","test_mode", logZones.toString());
        return handlerInput.getResponseBuilder()
                .withSpeech(zones.toString())
                .addDirective(MindstormAlexaUtil.buildDirective(endpointId, MindstormAlexaUtil.NAMESPACE, MindstormAlexaUtil.NAME_CONTROL, payload))
                .withReprompt(i18n(locale, "reprompt"))
                .withShouldEndSession(false)
                .build();
    }
}
