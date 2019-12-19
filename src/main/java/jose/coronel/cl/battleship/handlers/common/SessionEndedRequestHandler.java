package jose.coronel.cl.battleship.handlers.common;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.Response;
import com.amazon.ask.model.SessionEndedRequest;

import java.util.Optional;

import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

import static com.amazon.ask.request.Predicates.requestType;

public class SessionEndedRequestHandler implements RequestHandler {

    private static Logger log = getLogger(SessionEndedRequestHandler.class);

    @Override
    public boolean canHandle(HandlerInput input) {
        return input.matches(requestType(SessionEndedRequest.class));
    }

    @Override
    public Optional<Response> handle(HandlerInput input) {
        log.info("Executing SessionEndedRequestHandler");

        SessionEndedRequest request = (SessionEndedRequest) input.getRequestEnvelope().getRequest();
        log.info("Reason to close the session : {}", request.getReason());
        if( null != request.getError()) {
            log.error("****************************");
            log.error("* Error reason  : {}", request.getReason());
            log.error("* Error Type    : {}", request.getError().getType());
            log.error("* Error Message : {}", request.getError().getMessage());
            log.error("****************************");
        }
        return input.getResponseBuilder().build();
    }

}

