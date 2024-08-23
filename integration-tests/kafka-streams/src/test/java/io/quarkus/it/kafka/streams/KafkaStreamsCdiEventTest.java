package io.quarkus.it.kafka.streams;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;

@WithTestResource(value = KafkaSSLTestResource.class, restrictToAnnotatedClass = false)
@QuarkusTest
public class KafkaStreamsCdiEventTest {

    @Inject
    KafkaStreamsEventCounter eventCounter;

    @Test
    void testEventShouldBePublished() {
        Assertions.assertEquals(1, eventCounter.getEventCount(),
                "There should be one event for creating KafakStreams in the producder.");
    }
}
