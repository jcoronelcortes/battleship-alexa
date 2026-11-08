package jose.coronel.cl.battleship.handlers.game;

import com.amazon.ask.model.Response;
import jose.coronel.cl.battleship.game.BattleshipGame;
import jose.coronel.cl.battleship.handlers.GameIntentHandler;
import jose.coronel.cl.battleship.handlers.Turn;
import jose.coronel.cl.battleship.robot.RobotCommand;

import java.util.Optional;

/** "Comenzar juego": new game, Alexa places her ships and the robot goes home. */
public class StartGameHandler extends GameIntentHandler {

    @Override
    protected String intentKey() {
        return "startGameIntent";
    }

    @Override
    protected Optional<Response> handle(Turn turn) {
        turn.session().save(new BattleshipGame().startGame());
        turn.session().clearPendingAlexaShot();
        turn.sendToRobot(RobotCommand.of(RobotCommand.START_GAME));
        return turn.ask(turn.text("startGameSpeechText",
                "<audio src=\"soundbank://soundlibrary/ui/gameshow/amzn_ui_sfx_gameshow_intro_01\"/>"));
    }

}
