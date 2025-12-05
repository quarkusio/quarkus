package io.quarkus.it.kafka;

import org.junit.jupiter.api.BeforeAll;

import io.apicurio.registry.resolver.client.RegistryClientFacadeFactory;
import io.quarkus.it.kafka.avro.AvroKafkaCreator;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.vertx.core.Vertx;

@QuarkusIntegrationTest
@QuarkusTestResource(value = KafkaResource.class, restrictToAnnotatedClass = true)
public class KafkaAvroIT extends KafkaAvroTestBase {

    AvroKafkaCreator creator;

    @Override
    AvroKafkaCreator creator() {
        return creator;
    }

    @BeforeAll
    public static void setUp() {
        // this is for the test JVM, which also uses Kafka client, which in turn also interacts with the registry
        RegistryClientFacadeFactory.vertx = Vertx.vertx();
    }

}
