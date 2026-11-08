package jose.coronel.cl.battleship.handlers.game;

import com.amazon.ask.model.Response;
import jose.coronel.cl.battleship.game.board.Game;
import jose.coronel.cl.battleship.handlers.ActiveGameHandler;
import jose.coronel.cl.battleship.handlers.Turn;
import jose.coronel.cl.battleship.robot.RobotCommand;
import jose.coronel.cl.battleship.util.GameModeUtil;

import java.util.Optional;

/** "Modo de juego avanzado": sets the difficulty level. */
public class GameModeHandler extends ActiveGameHandler {

    @Override
    protected String intentKey() {
        return "gameModeIntent";
    }

    @Override
    protected Optional<Response> handle(Turn turn, Game game) {
        String level = turn.slot("gameMode");
        if (level == null) {
            level = turn.text("defaultGameMode");
        }
        game.setGameMode(GameModeUtil.getGameMode(level));
        turn.session().save(game);
        turn.sendToRobot(RobotCommand.of(RobotCommand.GAME_MODE_CONFIGURED));
        return turn.ask(turn.text("gameModeSpeechText", level.toLowerCase(turn.locale())));
    }

}
