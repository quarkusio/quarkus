package io.quarkus.it.kafka;

import jakarta.inject.Inject;

import io.quarkus.it.kafka.avro.AvroKafkaCreator;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class KafkaAvroTest extends KafkaAvroTestBase {

    @Inject
    AvroKafkaCreator creator;

    @Override
    AvroKafkaCreator creator() {
        return creator;
    }
}
