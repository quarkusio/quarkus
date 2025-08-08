package io.quarkus.it.kafka;

import org.junit.jupiter.api.Test;

import io.quarkus.it.kafka.avro.AvroKafkaCreator;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@QuarkusTestResource(value = KafkaResource.class, restrictToAnnotatedClass = true)
public class KafkaAvroWithTestResourceTest extends KafkaAvroTestBase {

    // This will be injected by the test resource, which is similar in operation to the KafkaCompanion
    AvroKafkaCreator creator;

    @Override
    AvroKafkaCreator creator() {
        return creator;
    }

    @Test
    public void testConfluentAvroConsumer() {
        // Hacky disabling; there's some cross-talk with the KafkaAvroTest causing socket timeouts, so just don't run this test for now
    }

    @Test
    public void testApicurioAvroConsumer() {
        // Hacky disabling; there's some cross-talk with the KafkaAvroTest causing socket timeouts, so just don't run this test for now
    }

}
