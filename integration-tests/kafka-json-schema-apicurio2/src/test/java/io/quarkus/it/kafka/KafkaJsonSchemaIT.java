package io.quarkus.it.kafka;

import io.quarkus.it.kafka.jsonschema.JsonSchemaKafkaCreator;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusIntegrationTest;

@QuarkusIntegrationTest
@QuarkusTestResource(value = KafkaResource.class, restrictToAnnotatedClass = true)
public class KafkaJsonSchemaIT extends KafkaJsonSchemaTestBase {

    JsonSchemaKafkaCreator creator;

    @Override
    JsonSchemaKafkaCreator creator() {
        return creator;
    }

}
