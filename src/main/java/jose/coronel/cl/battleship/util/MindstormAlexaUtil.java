package jose.coronel.cl.battleship.util;

import com.amazon.ask.attributes.AttributesManager;
import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.interfaces.customInterfaceController.Endpoint;
import com.amazon.ask.model.interfaces.customInterfaceController.Header;
import com.amazon.ask.model.interfaces.customInterfaceController.SendDirectiveDirective;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jose.coronel.cl.battleship.gadget.EveGadget;
import jose.coronel.cl.battleship.game.board.Game;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.Response;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.slf4j.LoggerFactory.getLogger;

public class MindstormAlexaUtil {

    private static Logger log = getLogger(MindstormAlexaUtil.class);

    public static String NAMESPACE = "Custom.Mindstorms.Gadget";
    public static String NAME_CONTROL = "control";


    public static EveGadget getConnectedEndpoints(String apiEndpoint, String apiAccessToken) throws Exception {
        log.info("Obteniendo endpoint del EV3 conectado");
        String url = apiEndpoint + "/v1/endpoints";
        log.info("URL : {}", url);
        log.info("Api Access Token : {}", apiAccessToken);
        AsyncHttpClient asyncHttpClient = new DefaultAsyncHttpClient();
        CompletableFuture<Response> response = asyncHttpClient
                .prepareGet(url)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + apiAccessToken)
                .execute()
                .toCompletableFuture();

        String jsonResponse = null;
        try {
            jsonResponse = response.get().getResponseBody();
        } catch (InterruptedException e) {
            log.error("Mensaje de error : {}", e.getMessage(), e);
            throw new Exception(e);
        } catch (ExecutionException e) {
            log.error("Mensaje de error : {}", e.getMessage(), e);
            throw new Exception(e);
        }
        log.info("Respuesta del EV3 : {}",  jsonResponse);
        try {
            return new ObjectMapper().readValue(jsonResponse, EveGadget.class);
        } catch (IOException e) {
            log.error("Mensaje de error : {}", e.getMessage(), e);
            throw new Exception(e);
        }
    }


    public static void putSessionAttribute(HandlerInput input, String key, String value) {
        AttributesManager attributesManager = input.getAttributesManager();
        Map<String, Object> sessionAttributes = attributesManager.getSessionAttributes();
        sessionAttributes.put(key, value);
        attributesManager.setSessionAttributes(sessionAttributes);
    }

    public static Object getValueFromSession(HandlerInput input, String key){
        return input.getAttributesManager().getSessionAttributes().get(key);
    }


    public static SendDirectiveDirective buildDirective(String endpointId, String namespace, String name, Object payload){
        return SendDirectiveDirective
                .builder()
                .withHeader(Header.builder().withName(name).withNamespace(namespace).build())
                .withEndpoint(Endpoint.builder().withEndpointId(endpointId).build())
                .withPayload(payload)
                .build();
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
        ObjectMapper mapper = new ObjectMapper();
        String jsopnGame = "";
        try {
            jsopnGame = mapper.writeValueAsString(game);
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
