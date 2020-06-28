package io.quarkus.smallrye.reactivemessaging.cloudevents.deployment;

import java.net.URI;
import java.util.UUID;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.reactive.messaging.cloudevents.CloudEventMessage;
import io.smallrye.reactive.messaging.cloudevents.CloudEventMessageBuilder;

public class CloudEventsTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @Test
    public void testCloudEventCreation() {
        final String payload = "TestPayload";

        final CloudEventMessage<String> cloudEventMessage = new CloudEventMessageBuilder<String>()
                .withId(UUID.randomUUID().toString())
                .withSource(URI.create("my-topic"))
                .withType("kafka-event")
                .withData(payload)
                .build();

        Assertions.assertEquals(cloudEventMessage.getPayload(), payload);
    }
}
