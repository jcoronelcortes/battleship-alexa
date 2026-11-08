package jose.coronel.cl.battleship.robot;

import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Chooses the robot channel from Lambda environment variables:
 * <ul>
 *   <li>IOT_ENDPOINT: AWS IoT data endpoint. If missing, dummy mode is used (no robot).</li>
 *   <li>ROBOT_TOPIC: MQTT topic, default "battleship/ev3".</li>
 * </ul>
 */
public final class RobotChannels {

    private static final Logger log = getLogger(RobotChannels.class);
    public static final String DEFAULT_TOPIC = "battleship/ev3";

    private static RobotChannel instance;

    private RobotChannels() {
    }

    public static synchronized RobotChannel get() {
        if (instance == null) {
            instance = fromEnvironment();
        }
        return instance;
    }

    /** Replaces the channel, used by tests. */
    public static synchronized void set(RobotChannel channel) {
        instance = channel;
    }

    private static RobotChannel fromEnvironment() {
        String endpoint = System.getenv("IOT_ENDPOINT");
        if (endpoint == null || endpoint.isBlank()) {
            log.warn("IOT_ENDPOINT is not set: playing in dummy mode without robot");
            return new LoggingRobotChannel();
        }
        String topic = System.getenv().getOrDefault("ROBOT_TOPIC", DEFAULT_TOPIC);
        log.info("Robot channel: AWS IoT Core endpoint {} topic {}", endpoint, topic);
        try {
            return new MqttRobotChannel(endpoint, topic);
        } catch (Exception e) {
            // Keep the voice game alive even if the robot link is misconfigured
            log.error("Could not create AWS IoT client, playing in dummy mode: {}", e.getMessage(), e);
            return new LoggingRobotChannel();
        }
    }

}
