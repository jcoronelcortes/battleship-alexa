package jose.coronel.cl.battleship.util;

import com.amazon.ask.attributes.AttributesManager;
import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.IntentRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jose.coronel.cl.battleship.game.board.Game;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

public class MindstormAlexaUtil {

    private static Logger log = getLogger(MindstormAlexaUtil.class);

    public static void putSessionAttribute(HandlerInput input, String key, String value) {
        AttributesManager attributesManager = input.getAttributesManager();
        Map<String, Object> sessionAttributes = attributesManager.getSessionAttributes();
        sessionAttributes.put(key, value);
        attributesManager.setSessionAttributes(sessionAttributes);
    }

    public static Object getValueFromSession(HandlerInput input, String key){
        return input.getAttributesManager().getSessionAttributes().get(key);
    }


    public static Game jsonToGame(String jsonGame){
        Game game = null;
        try {
            game = new ObjectMapper().readValue(jsonGame, Game.class);
        }
        catch (IOException e) {
            log.error("Can't process JSON Game {} to Game. Error message {}", jsonGame, e.getMessage(), e);
        }
        return game;
    }

    public static String gameToJson(Game game){
        String jsopnGame = "";
        try {
            jsopnGame = new ObjectMapper().writeValueAsString(game);
        }
        catch (JsonProcessingException e) {
            log.error("Can't process game status to JSON . Error message : " + e.getMessage(), e);
        }
        return jsopnGame;
    }

    public static String getSlotFromIntentRequest(IntentRequest intentRequest, String slotName){
        return intentRequest.getIntent().getSlots().get(slotName).getValue();
    }

}
