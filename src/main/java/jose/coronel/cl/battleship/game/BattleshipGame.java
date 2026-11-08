package jose.coronel.cl.battleship.game;

import jose.coronel.cl.battleship.game.ai.AttackStrategy;
import jose.coronel.cl.battleship.game.board.Attack;
import jose.coronel.cl.battleship.game.board.BoardGame;
import jose.coronel.cl.battleship.game.board.Game;
import jose.coronel.cl.battleship.game.board.Ship;
import jose.coronel.cl.battleship.game.board.ShipType;
import jose.coronel.cl.battleship.robot.RobotCommand;
import org.slf4j.Logger;

import java.util.Random;

import static org.slf4j.LoggerFactory.getLogger;

/** Game rules: player shots on Alexa's board, Alexa's shots on the player's board. No Alexa/voice code here. */
public class BattleshipGame {

    private static final Logger log = getLogger(BattleshipGame.class);

    private final Random random;

    public BattleshipGame() {
        this(new Random());
    }

    public BattleshipGame(Random random) {
        this.random = random;
    }

    public Game startGame() {
        Game game = Game.newGame(random);
        log.info("New game created");
        return game;
    }

    /**
     * The player shoots Alexa's board at [row, column] (0–8).
     * @return the command for the robot, with the result (impact, sunk, fail, won, duplicate)
     */
    public RobotCommand executePlayerAttack(Game game, int row, int column) {
        BoardGame alexaBoard = game.getBoardGameAlexa();
        RobotCommand command = RobotCommand.playerAttack(row, column);

        if (alexaBoard.wasAttacked(row, column)) {
            log.info("Zone [{}, {}] was already attacked", row, column);
            command.setAttackResult(RobotCommand.DUPLICATE_ATTACK);
        } else if (alexaBoard.hasShip(row, column)) {
            alexaBoard.recordImpact(row, column);
            Ship ship = game.shipByIdentifier(alexaBoard.shipIdentifierAt(row, column));
            if (ship.hit()) {
                log.info("Player sank Alexa's {}", ship.getTypeOfShip());
                command.setAttackResult(RobotCommand.SHIP_DESTROYED);
                command.setShipDestroyed(ship.getTypeOfShip());
            } else {
                log.info("Player hit a ship at [{}, {}]", row, column);
                command.setAttackResult(RobotCommand.IMPACT);
            }
        } else {
            alexaBoard.recordFailedImpact(row, column);
            log.info("Player missed at [{}, {}]", row, column);
            command.setAttackResult(RobotCommand.FAIL);
        }

        if (alexaBoard.allShipsHit()) {
            log.info("The player won the game");
            command.setAttackResult(RobotCommand.PLAYER_WON);
        }
        return command;
    }

    /** Alexa's next shot on the player's board, chosen by the difficulty level. */
    public Attack generateAlexaAttack(Game game) {
        Attack shot = AttackStrategy.forMode(game.getGameMode()).nextShot(game.getBoardGamePlayer(), random);
        log.info("Alexa shoots [{}, {}] (mode {})", shot.getRow(), shot.getColumn(), game.getGameMode());
        if (log.isDebugEnabled()) {
            log.debug("Player board:\n{}", game.getBoardGamePlayer().toDebugString());
        }
        return shot;
    }

    /** The player says "impact": Alexa hit a ship. */
    public void recordAlexaHit(Game game, int row, int column) {
        game.getBoardGamePlayer().recordImpact(row, column);
        AttackStrategy.forMode(game.getGameMode()).onHit(game.getBoardGamePlayer(), row, column);
    }

    /** The player says "you fail". */
    public void recordAlexaMiss(Game game, int row, int column) {
        game.getBoardGamePlayer().recordFailedImpact(row, column);
    }

    /**
     * The player says Alexa sank a ship.
     * @param type the ship, or null when the player did not say which one (it is guessed)
     * @return how many of the player's ships are still afloat
     */
    public int recordAlexaSunk(Game game, ShipType type, int row, int column) {
        log.info("Alexa sank the player's {}", type != null ? type : "ship (not named, it will be guessed)");
        return game.getBoardGamePlayer().markSunk(type, row, column);
    }

}
