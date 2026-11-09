package jose.coronel.cl.battleship.simulador;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.crt.mqtt5.Mqtt5Client;
import software.amazon.awssdk.crt.mqtt5.Mqtt5ClientOptions;
import software.amazon.awssdk.crt.mqtt5.OnAttemptingConnectReturn;
import software.amazon.awssdk.crt.mqtt5.OnConnectionFailureReturn;
import software.amazon.awssdk.crt.mqtt5.OnConnectionSuccessReturn;
import software.amazon.awssdk.crt.mqtt5.OnDisconnectionReturn;
import software.amazon.awssdk.crt.mqtt5.OnStoppedReturn;
import software.amazon.awssdk.crt.mqtt5.PublishReturn;
import software.amazon.awssdk.crt.mqtt5.QOS;
import software.amazon.awssdk.crt.mqtt5.packets.ConnectPacket;
import software.amazon.awssdk.crt.mqtt5.packets.SubscribePacket;
import software.amazon.awssdk.iot.AwsIotMqtt5ClientBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * The virtual robot on AWS IoT: connects with the EV3's certificates and listens to the same topic,
 * so it receives exactly what the real robot would. Only one of them can be connected at a time
 * (same client id): do not run it while the real EV3 is on.
 */
@Component
public class IotRobot {

    private static final Logger log = LoggerFactory.getLogger(IotRobot.class);

    private final RobotEvents events;
    private final Path certs;
    private Mqtt5Client client;
    private volatile String status = "desconectado";

    public IotRobot(RobotEvents events, @Value("${simulador.robot-certs}") String certs) {
        this.events = events;
        this.certs = Path.of(certs);
    }

    public String status() {
        return status;
    }

    public synchronized void connect() {
        if (client != null) {
            return;
        }
        Map<String, String> env = readEnv(certs.resolve("robot.env"));
        String topic = env.get("TOPIC");
        AwsIotMqtt5ClientBuilder builder = AwsIotMqtt5ClientBuilder.newDirectMqttBuilderWithMtlsFromPath(
                env.get("IOT_ENDPOINT"),
                certs.resolve("certificate.pem.crt").toString(),
                certs.resolve("private.pem.key").toString());
        builder.withCertificateAuthorityFromPath(null, certs.resolve("AmazonRootCA1.pem").toString());
        builder.withConnectProperties(new ConnectPacket.ConnectPacketBuilder().withClientId(env.get("CLIENT_ID")));
        builder.withLifeCycleEvents(new Mqtt5ClientOptions.LifecycleEvents() {
            @Override
            public void onAttemptingConnect(Mqtt5Client c, OnAttemptingConnectReturn r) {
                status = "conectando";
            }

            @Override
            public void onConnectionSuccess(Mqtt5Client c, OnConnectionSuccessReturn r) {
                status = "conectado";
                c.subscribe(new SubscribePacket.SubscribePacketBuilder()
                        .withSubscription(topic, QOS.AT_LEAST_ONCE).build());
                log.info("Robot virtual conectado a AWS IoT, escuchando {}", topic);
            }

            @Override
            public void onConnectionFailure(Mqtt5Client c, OnConnectionFailureReturn r) {
                status = "error de conexión";
                log.warn("No se pudo conectar a AWS IoT: código {}", r.getErrorCode());
            }

            @Override
            public void onDisconnection(Mqtt5Client c, OnDisconnectionReturn r) {
                status = "desconectado";
            }

            @Override
            public void onStopped(Mqtt5Client c, OnStoppedReturn r) {
                status = "desconectado";
            }
        });
        builder.withPublishEvents((Mqtt5Client c, PublishReturn message) -> events.add("aws-iot",
                new String(message.getPublishPacket().getPayload(), StandardCharsets.UTF_8)));
        client = builder.build();
        builder.close();
        client.start();
    }

    public synchronized void disconnect() {
        if (client != null) {
            client.stop();
            client.close();
            client = null;
        }
        status = "desconectado";
    }

    private static Map<String, String> readEnv(Path file) {
        Map<String, String> values = new HashMap<>();
        try {
            for (String line : Files.readAllLines(file)) {
                int equals = line.indexOf('=');
                if (equals > 0 && !line.startsWith("#")) {
                    values.put(line.substring(0, equals).trim(), line.substring(equals + 1).trim());
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("No encuentro " + file + ". Ejecuta primero deploy/deploy.sh", e);
        }
        return values;
    }

}
