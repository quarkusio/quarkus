package io.quarkus.it.kafka;

import org.junit.jupiter.api.BeforeAll;

import io.apicurio.registry.rest.client.RegistryClientFactory;
import io.apicurio.rest.client.VertxHttpClientProvider;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.vertx.core.Vertx;

@QuarkusIntegrationTest
@QuarkusTestResource(KafkaAndSchemaRegistryTestResource.class)
public class KafkaAvroIT extends KafkaAvroTest {
    @BeforeAll
    public static void setUp() {
        // this is for the test JVM, which also uses Kafka client, which in turn also interacts with the registry
        RegistryClientFactory.setProvider(new VertxHttpClientProvider(Vertx.vertx()));
    }

}
