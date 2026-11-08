package jose.coronel.cl.battleship.robot;

import jose.coronel.cl.battleship.handlers.ev3.directives.SendCommandDirective;

/**
 * Sends game commands (start_game, player_attack, ...) to the EV3 robot.
 * Failures must never break the voice response: implementations log and continue.
 */
public interface RobotChannel {

    void send(SendCommandDirective command);

}
