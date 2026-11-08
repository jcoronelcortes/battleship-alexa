package jose.coronel.cl.battleship.handlers.game;

import com.amazon.ask.model.Response;
import jose.coronel.cl.battleship.game.BattleshipGame;
import jose.coronel.cl.battleship.game.board.Attack;
import jose.coronel.cl.battleship.game.board.Game;
import jose.coronel.cl.battleship.game.board.ShipType;
import jose.coronel.cl.battleship.handlers.ActiveGameHandler;
import jose.coronel.cl.battleship.handlers.Turn;
import jose.coronel.cl.battleship.robot.RobotCommand;
import jose.coronel.cl.battleship.util.AttackBoardTime;
import jose.coronel.cl.battleship.util.Coordinates;

import java.util.Optional;

/**
 * "C cinco": the player shoots Alexa's board, the robot marks the shot, then Alexa shoots back.
 * The speech has pauses so Alexa talks while the robot moves (AttackBoardTime).
 */
public class PlayerAttackHandler extends ActiveGameHandler {

    private static final String CANNON = "<audio src=\"soundbank://soundlibrary/guns/cannons/cannons_02\"/>";

    @Override
    protected String intentKey() {
        return "playerAttackIntent";
    }

    @Override
    protected Optional<Response> handle(Turn turn, Game game) {
        int row = Coordinates.rowFromSpoken(turn.slot("row"));
        int column = Coordinates.columnFromSpoken(turn.slot("column"));
        if (!Coordinates.isInside(row, column)) {
            return turn.ask(turn.text("invalidCellSpeechText"));
        }
        Optional<Attack> unanswered = turn.session().pendingAlexaShot();
        if (unanswered.isPresent()) {
            Attack shot = unanswered.get();
            return turn.ask(turn.text("answerMyShotFirstSpeechText",
                    Coordinates.rowLetter(shot.getRow()), String.valueOf(shot.getColumn() + 1)));
        }

        BattleshipGame engine = new BattleshipGame();
        RobotCommand attack = engine.executePlayerAttack(game, row, column);

        if (attack.isPlayerWon()) {
            turn.session().save(game);
            // First the robot marks the winning shot, then it celebrates
            turn.sendToRobot(attack);
            turn.sendToRobot(RobotCommand.of(RobotCommand.FINISH_GAME));
            return turn.tell(turn.text("impactYouWin"));
        }

        Attack alexaShot = engine.generateAlexaAttack(game);
        String rowLetter = Coordinates.rowLetter(alexaShot.getRow());
        String columnNumber = String.valueOf(alexaShot.getColumn() + 1);
        attack.setAlexaAttack(rowLetter, alexaShot.getColumn() + 1);
        turn.session().setPendingAlexaShot(alexaShot);
        turn.session().save(game);
        turn.sendToRobot(attack);

        return turn.ask(attackResultSpeech(turn, attack, row, column)
                + turn.text("myTurnToAttack")
                + turn.text("alexaAttackSpeechText", rowLetter, columnNumber));
    }

    private String attackResultSpeech(Turn turn, RobotCommand attack, int row, int column) {
        String filler = turn.text("aimingFiller");
        String before = AttackBoardTime.getSsmlBreaksOnMillisecondsBeforeToAttack(row, column, filler);
        String after = AttackBoardTime.getSsmlBreaksOnMillisecondsAfterToAttack(row, column, filler);
        switch (attack.getAttackResult()) {
            case RobotCommand.IMPACT:
                return turn.text("impactAttack" + random(4), before, CANNON, after);
            case RobotCommand.SHIP_DESTROYED:
                String ship = ShipType.fromSpoken(attack.getShipDestroyed())
                        .map(type -> turn.text(type.nameKey())).orElse(attack.getShipDestroyed());
                return turn.text("impactDestroyed", before, CANNON, ship, after);
            case RobotCommand.FAIL:
                return turn.text("impactFail" + random(4), before, CANNON, after);
            case RobotCommand.DUPLICATE_ATTACK:
                return turn.text("zoneAlreadyAttacked");
            default:
                return "";
        }
    }

    private static int random(int variants) {
        return (int) (Math.random() * variants) + 1;
    }

}
