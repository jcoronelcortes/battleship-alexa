package jose.coronel.cl.battleship.simulador;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Commands received by the virtual robot, in order. The page polls them with "since id". */
@Component
public class RobotEvents {

    public record Event(int id, String source, String json, Instant received) {
    }

    private final List<Event> events = new ArrayList<>();

    public synchronized void add(String source, String json) {
        events.add(new Event(events.size() + 1, source, json, Instant.now()));
    }

    public synchronized List<Event> since(int id) {
        return id >= events.size() ? List.of() : List.copyOf(events.subList(Math.max(0, id), events.size()));
    }

}
