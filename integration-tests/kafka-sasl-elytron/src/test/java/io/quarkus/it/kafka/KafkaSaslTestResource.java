package io.quarkus.it.kafka;

import static io.strimzi.test.container.StrimziKafkaContainer.KAFKA_PORT;

import java.util.HashMap;
import java.util.Map;

import org.jboss.logging.Logger;
import org.testcontainers.utility.MountableFile;

import io.quarkus.it.kafka.containers.KerberosContainer;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.strimzi.test.container.StrimziKafkaContainer;

public class KafkaSaslTestResource implements QuarkusTestResourceLifecycleManager {

    private final Logger log = Logger.getLogger(KafkaSaslTestResource.class);

    private StrimziKafkaContainer kafka;
    private KerberosContainer kerberos;

    @Override
    public Map<String, String> start() {

        Map<String, String> properties = new HashMap<>();

        //Start kerberos container
        kerberos = new KerberosContainer("gcavalcante8808/krb5-server");
        kerberos.start();
        log.info(kerberos.getLogs());
        kerberos.createTestPrincipals();
        kerberos.createKrb5File();
        properties.put("java.security.krb5.conf", "src/test/resources/krb5.conf");

        //Start kafka container
        kafka = new StrimziKafkaContainer()
                .withBootstrapServers(
                        c -> String.format("SASL_PLAINTEXT://%s:%s", c.getHost(), c.getMappedPort(KAFKA_PORT)))
                .withPort(KAFKA_PORT)
                .withServerProperties(MountableFile.forClasspathResource("kafkaServer.properties"))
                .withCopyFileToContainer(MountableFile.forClasspathResource("krb5KafkaBroker.conf"),
                        "/etc/krb5.conf")
                .withCopyFileToContainer(MountableFile.forHostPath("target/kafkabroker.keytab"),
                        "/opt/kafka/config/kafkabroker.keytab");
        kafka.start();
        log.info(kafka.getLogs());
        properties.put("kafka.bootstrap.servers", kafka.getBootstrapServers());

        return properties;
    }

    @Override
    public void stop() {

        if (kafka != null) {
            kafka.close();
            kafka.stop();
        }

        if (kerberos != null) {
            kerberos.close();
            kerberos.stop();
        }

    }
}
