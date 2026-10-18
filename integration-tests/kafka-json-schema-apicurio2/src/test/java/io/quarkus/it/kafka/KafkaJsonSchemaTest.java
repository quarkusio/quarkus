package io.quarkus.it.kafka;

import jakarta.inject.Inject;

import io.quarkus.it.kafka.jsonschema.JsonSchemaKafkaCreator;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class KafkaJsonSchemaTest extends KafkaJsonSchemaTestBase {

    @Inject
    JsonSchemaKafkaCreator creator;

    @Override
    JsonSchemaKafkaCreator creator() {
        return creator;
    }
}
