package jose.coronel.cl.battleship.simulador;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Web simulator: play Battleship against the skill (local or on AWS) and act as the virtual robot. */
@SpringBootApplication
public class SimuladorApplication {

    public static void main(String[] args) {
        SpringApplication.run(SimuladorApplication.class, args);
    }

}
