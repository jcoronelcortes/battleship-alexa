package jose.coronel.cl.battleship.handlers.game;

import com.amazon.ask.model.Response;
import jose.coronel.cl.battleship.handlers.GameIntentHandler;
import jose.coronel.cl.battleship.handlers.Turn;
import jose.coronel.cl.battleship.robot.RobotCommand;

import java.util.Optional;

/** "Tú ganas": the player gives up or says Alexa won. Ends the game. */
public class AlexaWinsTheGameHandler extends GameIntentHandler {

    @Override
    protected String intentKey() {
        return "alexaWinsTheGameIntent";
    }

    @Override
    protected Optional<Response> handle(Turn turn) {
        turn.sendToRobot(RobotCommand.of(RobotCommand.FINISH_GAME));
        return turn.tell(turn.text("alexaWinsSpeechText"));
    }

}
