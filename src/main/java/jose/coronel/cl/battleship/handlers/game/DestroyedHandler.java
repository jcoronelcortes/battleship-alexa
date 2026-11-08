package jose.coronel.cl.battleship.handlers.game;

import com.amazon.ask.model.Response;
import jose.coronel.cl.battleship.game.BattleshipGame;
import jose.coronel.cl.battleship.game.board.Attack;
import jose.coronel.cl.battleship.game.board.Game;
import jose.coronel.cl.battleship.game.board.ShipType;
import jose.coronel.cl.battleship.handlers.AlexaShotAnswerHandler;
import jose.coronel.cl.battleship.handlers.Turn;
import jose.coronel.cl.battleship.robot.RobotCommand;

import java.util.Optional;

/** "Hundiste mi submarino" (or just "hundido"): Alexa's shot sank a ship. If it was the last one, Alexa wins. */
public class DestroyedHandler extends AlexaShotAnswerHandler {

    @Override
    protected String intentKey() {
        return "destroyedIntent";
    }

    @Override
    protected Optional<Response> handle(Turn turn, Game game, Attack alexaShot) {
        // Unknown or missing ship name: the engine guesses it from the hits
        ShipType ship = ShipType.fromSpoken(turn.slot("ship")).orElse(null);
        int pendingShips = new BattleshipGame().recordAlexaSunk(game, ship, alexaShot.getRow(), alexaShot.getColumn());
        turn.session().save(game);

        if (pendingShips == 0) {
            turn.sendToRobot(RobotCommand.of(RobotCommand.FINISH_GAME));
            return turn.tell(turn.text("alexaSankAllShipsSpeechText") + " " + turn.text("alexaWinsSpeechText"));
        }
        turn.sendToRobot(RobotCommand.of(RobotCommand.ALEXA_SHIP_DESTROYED));
        return turn.ask(turn.text("alexaDestroySpeechText", turn.text("shipLengthDestroySpeechText" + pendingShips)));
    }

}
