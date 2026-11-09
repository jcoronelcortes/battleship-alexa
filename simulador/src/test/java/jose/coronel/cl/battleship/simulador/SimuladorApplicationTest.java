package jose.coronel.cl.battleship.simulador;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The Spring application starts and the API answers. */
@SpringBootTest
class SimuladorApplicationTest {

    @Autowired
    private SimulatorController controller;

    @Test
    void startsInLocalModeWithTheRobotDisconnected() {
        GameSimulator.State state = controller.state();
        assertEquals(GameSimulator.Mode.LOCAL, state.mode());
        assertEquals("local", state.robot());
    }

}
