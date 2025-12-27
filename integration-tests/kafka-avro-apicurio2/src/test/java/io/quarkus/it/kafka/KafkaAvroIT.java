package io.quarkus.it.kafka;

import io.quarkus.it.kafka.avro.AvroKafkaCreator;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusIntegrationTest;

@QuarkusIntegrationTest
@QuarkusTestResource(value = KafkaResource.class, restrictToAnnotatedClass = true)
public class KafkaAvroIT extends KafkaAvroTestBase {

    AvroKafkaCreator creator;

    @Override
    AvroKafkaCreator creator() {
        return creator;
    }

}
