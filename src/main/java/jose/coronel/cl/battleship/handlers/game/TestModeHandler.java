package jose.coronel.cl.battleship.handlers.game;

import com.amazon.ask.model.Response;
import jose.coronel.cl.battleship.game.board.BoardGame;
import jose.coronel.cl.battleship.game.board.Game;
import jose.coronel.cl.battleship.handlers.ActiveGameHandler;
import jose.coronel.cl.battleship.handlers.Turn;
import jose.coronel.cl.battleship.robot.RobotCommand;
import jose.coronel.cl.battleship.util.Coordinates;

import java.util.Optional;

/** "Activar modo prueba": Alexa reads where her ships are (for testing the robot). */
public class TestModeHandler extends ActiveGameHandler {

    @Override
    protected String intentKey() {
        return "testModeIntent";
    }

    @Override
    protected Optional<Response> handle(Turn turn, Game game) {
        StringBuilder speech = new StringBuilder();
        StringBuilder log = new StringBuilder();
        BoardGame alexaBoard = game.getBoardGameAlexa();
        for (int row = 0; row < BoardGame.NUMBER_OF_ROWS; row++) {
            for (int column = 0; column < BoardGame.NUMBER_OF_COLUMNS; column++) {
                if (alexaBoard.hasShip(row, column)) {
                    String letter = Coordinates.rowLetter(row);
                    int number = column + 1;
                    speech.append("<p>").append(letter).append("</p><break time=\"100ms\"/><p>")
                            .append(number).append("</p><break time=\"100ms\"/>");
                    log.append("[ ").append(letter).append(" , ").append(number).append(" ] ");
                }
            }
        }
        turn.sendToRobot(RobotCommand.testMode(log.toString()));
        return turn.ask(speech.toString());
    }

}
