package io.quarkus.it.kafka;

import java.util.HashMap;
import java.util.Map;

import org.jboss.logging.Logger;

import io.quarkus.it.kafka.containers.KafkaContainer;
import io.quarkus.it.kafka.containers.KerberosContainer;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class KafkaSaslTestResource implements QuarkusTestResourceLifecycleManager {

    private final Logger log = Logger.getLogger(KafkaSaslTestResource.class);

    private KafkaContainer kafka;
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
        kafka = new KafkaContainer();
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
