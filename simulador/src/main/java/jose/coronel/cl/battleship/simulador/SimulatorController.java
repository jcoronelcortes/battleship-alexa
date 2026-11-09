package jose.coronel.cl.battleship.simulador;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** JSON API used by the page (src/main/resources/static). */
@RestController
@RequestMapping("/api")
public class SimulatorController {

    /** What the player says: the spoken text (for the log), the intent and its slots. */
    public record Say(String spoken, String intent, Map<String, String> slots) {
    }

    private final GameSimulator simulator;
    private final RobotEvents robotEvents;
    private final IotRobot iotRobot;

    public SimulatorController(GameSimulator simulator, RobotEvents robotEvents, IotRobot iotRobot) {
        this.simulator = simulator;
        this.robotEvents = robotEvents;
        this.iotRobot = iotRobot;
    }

    @GetMapping("/estado")
    public GameSimulator.State state() {
        return simulator.state();
    }

    @PostMapping("/abrir")
    public GameSimulator.State open() {
        return simulator.open();
    }

    @PostMapping("/decir")
    public GameSimulator.State say(@RequestBody Say say) {
        return simulator.say(say.spoken(), say.intent(), say.slots() == null ? Map.of() : say.slots());
    }

    @PostMapping("/responder-auto")
    public GameSimulator.State autoAnswer() {
        return simulator.autoAnswer();
    }

    @PostMapping("/modo")
    public GameSimulator.State mode(@RequestParam GameSimulator.Mode valor) {
        simulator.setMode(valor);
        return simulator.state();
    }

    @PostMapping("/respuesta-automatica")
    public GameSimulator.State autoAnswer(@RequestParam boolean activa) {
        simulator.setAutoAnswer(activa);
        return simulator.state();
    }

    @PostMapping("/flota")
    public GameSimulator.State setFleet(@RequestBody List<PlayerFleet.Placement> placements) {
        simulator.setFleet(placements);
        return simulator.state();
    }

    @PostMapping("/flota/nueva")
    public GameSimulator.State newFleet() {
        simulator.newFleet();
        return simulator.state();
    }

    @GetMapping("/robot/eventos")
    public List<RobotEvents.Event> robotEvents(@RequestParam(defaultValue = "0") int desde) {
        return robotEvents.since(desde);
    }

    @PostMapping("/robot/conectar")
    public GameSimulator.State connectRobot() {
        iotRobot.connect();
        return simulator.state();
    }

    @PostMapping("/robot/desconectar")
    public GameSimulator.State disconnectRobot() {
        iotRobot.disconnect();
        return simulator.state();
    }

    /** Invalid fleet or a move that is not allowed now: the message is shown to the player. */
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<Map<String, String>> badRequest(RuntimeException e) {
        return ResponseEntity.badRequest().body(Map.of("error", String.valueOf(e.getMessage())));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> error(RuntimeException e) {
        return ResponseEntity.internalServerError().body(Map.of("error", String.valueOf(e.getMessage())));
    }

}
