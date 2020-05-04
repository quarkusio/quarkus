package io.quarkus.it.kafka;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import io.debezium.kafka.KafkaCluster;
import io.debezium.util.Testing;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class KafkaSASLTestResource implements QuarkusTestResourceLifecycleManager {

    private KafkaCluster kafka;

    @Override
    public Map<String, String> start() {
        try {
            File directory = Testing.Files.createTestingDirectory("kafka-data-sasl", true);

            enableServerJaasConf();

            Properties props = new Properties();
            props.setProperty("zookeeper.connection.timeout.ms", "45000");
            props.setProperty("listener.security.protocol.map", "CLIENT:SASL_PLAINTEXT");
            props.setProperty("listeners", "CLIENT://:19094");
            props.setProperty("inter.broker.listener.name", "CLIENT");

            props.setProperty("sasl.enabled.mechanisms", "PLAIN");
            props.setProperty("sasl.mechanism.inter.broker.protocol", "PLAIN");

            kafka = new KafkaCluster()
                    .withPorts(2184, 19094)
                    .addBrokers(1)
                    .usingDirectory(directory)
                    .deleteDataUponShutdown(true)
                    .withKafkaConfiguration(props)
                    .deleteDataPriorToStartup(true)
                    .startup();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return Collections.emptyMap();
    }

    @Override
    public void stop() {
        if (kafka != null) {
            kafka.shutdown();
        }
    }

    public static void enableServerJaasConf() throws IOException {
        final Path conf = Files.createTempFile("kafka-server-jaas.", ".conf");
        String serverConfiguration = "KafkaServer { "
                + "org.apache.kafka.common.security.plain.PlainLoginModule required "
                + "username=\"broker\" "
                + "password=\"broker-secret\" "
                + "user_broker=\"broker-secret\" "
                + "user_client=\"client-secret\"; };";

        Files.write(conf, ("client." + serverConfiguration).getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE);

        System.setProperty("java.security.auth.login.config", conf.toAbsolutePath().toString());
        System.setProperty("zookeeper.sasl.client", "false");
    }

    /**
     * Make sure this runs first otherwise system property {@code java.security.auth.login.config}
     * is ignored since {@link javax.security.auth.login.Configuration#configuration}
     * is already initialized with default, empty configuration.
     *
     * @return {@link Integer#MIN_VALUE}
     */
    @Override
    public int order() {
        return Integer.MIN_VALUE;
    }

}
