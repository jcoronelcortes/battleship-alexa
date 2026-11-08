package jose.coronel.cl.battleship.handlers.game;

import com.amazon.ask.model.Response;
import jose.coronel.cl.battleship.game.BattleshipGame;
import jose.coronel.cl.battleship.game.board.Attack;
import jose.coronel.cl.battleship.game.board.Game;
import jose.coronel.cl.battleship.handlers.AlexaShotAnswerHandler;
import jose.coronel.cl.battleship.handlers.Turn;
import jose.coronel.cl.battleship.robot.RobotCommand;

import java.util.Optional;

/** "Tocado": Alexa's shot hit one of the player's ships. */
public class ImpactAlexaHandler extends AlexaShotAnswerHandler {

    @Override
    protected String intentKey() {
        return "impactAlexaIntent";
    }

    @Override
    protected Optional<Response> handle(Turn turn, Game game, Attack alexaShot) {
        new BattleshipGame().recordAlexaHit(game, alexaShot.getRow(), alexaShot.getColumn());
        turn.session().save(game);
        turn.sendToRobot(RobotCommand.of(RobotCommand.ALEXA_IMPACT));
        return turn.ask(turn.randomText("impactItIsYourTurnSpeechText", 6));
    }

}
