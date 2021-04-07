package io.quarkus.it.kafka;

import static io.quarkus.it.kafka.KafkaTestResource.extract;
import static org.awaitility.Awaitility.await;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import org.apache.kafka.common.config.SslConfigs;

import io.debezium.kafka.KafkaCluster;
import io.debezium.util.Testing;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import kafka.server.KafkaServer;
import kafka.server.RunningAsBroker;

public class KafkaSSLTestResource implements QuarkusTestResourceLifecycleManager {

    private KafkaCluster kafka;

    @Override
    public Map<String, String> start() {
        try {
            File directory = Testing.Files.createTestingDirectory("kafka-data-ssl", true);
            File sslDir = sslDir(directory, true);

            Path ksPath = new File(sslDir, "kafka-keystore.p12").toPath();
            try (InputStream ksStream = getClass().getResourceAsStream("/kafka-keystore.p12")) {
                Files.copy(
                        ksStream,
                        ksPath,
                        StandardCopyOption.REPLACE_EXISTING);
            }

            Path tsPath = new File(sslDir, "kafka-truststore.p12").toPath();
            try (InputStream tsStream = getClass().getResourceAsStream("/kafka-truststore.p12")) {
                Files.copy(
                        tsStream,
                        tsPath,
                        StandardCopyOption.REPLACE_EXISTING);
            }
            String password = "Z_pkTh9xgZovK4t34cGB2o6afT4zZg0L";
            String type = "PKCS12";

            Properties props = new Properties();
            props.setProperty("zookeeper.connection.timeout.ms", "45000");
            //See http://kafka.apache.org/documentation.html#security_ssl for detail
            props.setProperty("listener.security.protocol.map", "CLIENT:SSL");
            props.setProperty("listeners", "CLIENT://:19099");
            props.setProperty("inter.broker.listener.name", "CLIENT");
            props.setProperty(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, ksPath.toString());
            props.setProperty(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, password);
            props.setProperty(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, type);
            props.setProperty(SslConfigs.SSL_KEY_PASSWORD_CONFIG, password);
            props.setProperty(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, tsPath.toString());
            props.setProperty(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, password);
            props.setProperty(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, type);
            props.setProperty(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "");

            kafka = new KafkaCluster()
                    .withPorts(2189, 19099)
                    .addBrokers(1)
                    .usingDirectory(directory)
                    .deleteDataUponShutdown(true)
                    .withKafkaConfiguration(props)
                    .deleteDataPriorToStartup(true)
                    .startup();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        KafkaServer server = extract(kafka);
        await().until(() -> server.brokerState().currentState() == RunningAsBroker.state());
        server.logger().underlying().info("Broker 'kafka-ssl' started");

        return Collections.emptyMap();
    }

    @Override
    public void stop() {
        if (kafka != null) {
            kafka.shutdown();
        }
    }

    public static File sslDir(File directory, boolean removeExistingContent) throws IOException {
        if (directory == null) {
            directory = Testing.Files.createTestingDirectory("kafka-data-ssl", removeExistingContent);
        }

        File targetDir = directory.getParentFile().getParentFile();
        File sslDir = new File(targetDir, "ssl_test");
        if (!sslDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            sslDir.mkdir();
        }
        return sslDir;
    }
}
