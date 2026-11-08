package jose.coronel.cl.battleship.handlers.game;

import com.amazon.ask.model.Response;
import jose.coronel.cl.battleship.game.BattleshipGame;
import jose.coronel.cl.battleship.game.board.Attack;
import jose.coronel.cl.battleship.game.board.Game;
import jose.coronel.cl.battleship.handlers.AlexaShotAnswerHandler;
import jose.coronel.cl.battleship.handlers.Turn;
import jose.coronel.cl.battleship.robot.RobotCommand;

import java.util.Optional;

/** "Al agua": Alexa's shot missed. */
public class YouFailAlexaHandler extends AlexaShotAnswerHandler {

    @Override
    protected String intentKey() {
        return "youFailAlexaIntent";
    }

    @Override
    protected Optional<Response> handle(Turn turn, Game game, Attack alexaShot) {
        new BattleshipGame().recordAlexaMiss(game, alexaShot.getRow(), alexaShot.getColumn());
        turn.session().save(game);
        turn.sendToRobot(RobotCommand.of(RobotCommand.FAIL_ATTACK));
        return turn.ask(turn.randomText("itIsYourTurnSpeechText", 3));
    }

}
