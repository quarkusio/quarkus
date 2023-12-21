package io.quarkus.it.kafka;

import jakarta.inject.Inject;

import io.quarkus.it.kafka.protobuf.ProtobufKafkaCreator;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class KafkaProtobufTest extends KafkaProtobufTestBase {

    @Inject
    ProtobufKafkaCreator creator;

    @Override
    ProtobufKafkaCreator creator() {
        return creator;
    }
}
