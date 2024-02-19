package io.quarkus.it.kafka;

import org.junit.jupiter.api.BeforeAll;

import io.apicurio.registry.rest.client.RegistryClientFactory;
import io.apicurio.rest.client.VertxHttpClientProvider;
import io.quarkus.it.kafka.protobuf.ProtobufKafkaCreator;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.vertx.core.Vertx;

@QuarkusIntegrationTest
@QuarkusTestResource(value = KafkaResource.class, restrictToAnnotatedClass = true)
public class KafkaProtobufTestIT extends KafkaProtobufTestBase {

    ProtobufKafkaCreator creator;

    @Override
    ProtobufKafkaCreator creator() {
        return creator;
    }

    @BeforeAll
    public static void setUp() {
        // this is for the test JVM, which also uses Kafka client, which in turn also interacts with the registry
        RegistryClientFactory.setProvider(new VertxHttpClientProvider(Vertx.vertx()));
    }

}
