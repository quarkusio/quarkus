package org.acme;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.reactive.messaging.providers.connectors.InMemoryConnector;
import io.smallrye.reactive.messaging.providers.connectors.InMemorySink;
import io.smallrye.reactive.messaging.providers.connectors.InMemorySource;
import org.junit.jupiter.api.Test;

import javax.enterprise.inject.Any;
import javax.inject.Inject;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@QuarkusTestResource(MyReactiveMessagingApplicationTest.InMemoryChannelTestResource.class)
class MyReactiveMessagingApplicationTest {

    @Inject
    @Any
    InMemoryConnector connector;

    @Test
    void test() {
        InMemorySource<String> source = connector.source("source-in");
        InMemorySink<String> uppercase = connector.sink("uppercase-out");

        source.send("Hello");
        source.send("In-memory");
        source.send("Connectors");

        assertEquals(3, uppercase.received().size());
        assertTrue(uppercase.received().stream().anyMatch(message -> message.getPayload().equals("HELLO")));
        assertTrue(uppercase.received().stream().anyMatch(message -> message.getPayload().equals("IN-MEMORY")));
        assertTrue(uppercase.received().stream().anyMatch(message -> message.getPayload().equals("CONNECTORS")));

    }

    public static class InMemoryChannelTestResource implements QuarkusTestResourceLifecycleManager {

        @Override
        public Map<String, String> start() {
            Map<String, String> env = new HashMap<>();
            env.putAll(InMemoryConnector.switchIncomingChannelsToInMemory("source-in"));
            env.putAll(InMemoryConnector.switchOutgoingChannelsToInMemory("uppercase-out"));
            return env;
        }

        @Override
        public void stop() {
            InMemoryConnector.clear();
        }
    }
}