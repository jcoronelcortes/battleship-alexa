package jose.coronel.cl.battleship.robot;

import com.fasterxml.jackson.databind.ObjectMapper;
import jose.coronel.cl.battleship.handlers.ev3.directives.SendCommandDirective;
import org.slf4j.Logger;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.iotdataplane.IotDataPlaneClient;
import software.amazon.awssdk.services.iotdataplane.model.PublishRequest;

import java.net.URI;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Publishes each command as JSON on an AWS IoT Core MQTT topic. The EV3 subscribes to that topic.
 */
public class MqttRobotChannel implements RobotChannel {

    private static final Logger log = getLogger(MqttRobotChannel.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final IotDataPlaneClient client;
    private final String topic;

    /**
     * @param iotEndpoint account data endpoint, e.g. "abc123-ats.iot.us-east-1.amazonaws.com"
     * @param topic       MQTT topic the EV3 listens on
     */
    public MqttRobotChannel(String iotEndpoint, String topic) {
        String url = iotEndpoint.startsWith("https://") ? iotEndpoint : "https://" + iotEndpoint;
        this.client = IotDataPlaneClient.builder()
                .endpointOverride(URI.create(url))
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .build();
        this.topic = topic;
    }

    @Override
    public void send(SendCommandDirective command) {
        try {
            String json = MAPPER.writeValueAsString(command);
            client.publish(PublishRequest.builder()
                    .topic(topic)
                    .qos(1)
                    .payload(SdkBytes.fromUtf8String(json))
                    .build());
            log.info("Command sent to robot on topic {} : {}", topic, json);
        } catch (Exception e) {
            log.error("Could not send command {} to robot: {}", command.getCommand(), e.getMessage(), e);
        }
    }

}
