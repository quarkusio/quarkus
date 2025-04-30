package io.quarkus.rest.client.reactive;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class ClientMultipleBodyParamLogMessageTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(Client.class));

    @Test
    void basicTest() {
        try {
            QuarkusRestClientBuilder.newBuilder().baseUri(URI.create("http://localhost:8081"))
                    .build(Client.class);
        } catch (Exception e) {
            assertThat(e.getMessage()).endsWith(
                    "Failed to generate client for class interface io.quarkus.rest.client.reactive.ClientMultipleBodyParamLogMessageTest$Client : Resource method 'io.quarkus.rest.client.reactive.ClientMultipleBodyParamLogMessageTest$Client#java.lang.String getMessagesForTopic(int param1, int param2)' can only have a single body parameter, but has at least 2. A body parameter is a method parameter without any annotations. Last discovered body parameter is 'param2'.");
            return;
        }
        Assertions.fail("Should have thrown an exception");
    }

    public interface Client {
        @GET
        @Path("/messages")
        String getMessagesForTopic(int param1, int param2);
    }
}
