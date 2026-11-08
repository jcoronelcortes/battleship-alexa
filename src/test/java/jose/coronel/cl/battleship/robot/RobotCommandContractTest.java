package jose.coronel.cl.battleship.robot;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The EV3 Python program (battleship-ev3/robot.py) reads these exact JSON fields.
 * Captured before the 2026 refactor: the format must not change.
 */
class RobotCommandContractTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void simpleCommandJson() throws Exception {
        assertEquals("{\"type\":\"command\",\"command\":\"start_game\",\"attackResult\":0,\"shipDestroyed\":\"NO\","
                        + "\"row\":null,\"column\":null,\"alexaRow\":null,\"alexaColumn\":null,\"logZones\":null}",
                mapper.writeValueAsString(RobotCommand.of(RobotCommand.START_GAME)));
    }

    @Test
    void playerAttackJson() throws Exception {
        RobotCommand attack = RobotCommand.playerAttack(2, 4);
        attack.setAttackResult(RobotCommand.SHIP_DESTROYED);
        attack.setShipDestroyed("Destroyer");
        attack.setAlexaAttack("E", 5);
        assertEquals("{\"type\":\"command\",\"command\":\"player_attack\",\"attackResult\":2,\"shipDestroyed\":\"Destroyer\","
                        + "\"row\":\"2\",\"column\":\"4\",\"alexaRow\":\"E\",\"alexaColumn\":\"5\",\"logZones\":null}",
                mapper.writeValueAsString(attack));
    }

}
