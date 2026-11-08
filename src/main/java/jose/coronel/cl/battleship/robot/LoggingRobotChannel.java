package jose.coronel.cl.battleship.robot;

import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * "Dummy mode": plays without a robot. Commands are only logged and kept for tests.
 */
public class LoggingRobotChannel implements RobotChannel {

    private static final Logger log = getLogger(LoggingRobotChannel.class);

    private final List<RobotCommand> sent = new ArrayList<>();

    @Override
    public void send(RobotCommand command) {
        sent.add(command);
        log.info("[dummy mode] Command for robot: {}", command.getCommand());
    }

    public List<RobotCommand> getSent() {
        return sent;
    }

}
