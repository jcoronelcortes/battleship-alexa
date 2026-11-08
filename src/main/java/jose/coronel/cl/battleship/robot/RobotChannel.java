package jose.coronel.cl.battleship.robot;

/**
 * Sends game commands (start_game, player_attack, ...) to the EV3 robot.
 * Failures must never break the voice response: implementations log and continue.
 */
public interface RobotChannel {

    void send(RobotCommand command);

}
