package org.jboss.resteasy.reactive.client.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Test;

public class ClientBuilderTest {

    @Test
    void configurationCopiedOnBuild() {
        ClientBuilder builder = ClientBuilder.newBuilder();

        Client firstClient = builder.build();
        firstClient.property("foo", "bar");
        firstClient.register(Always500.class);

        Client secondClient = builder.build();

        try (firstClient; secondClient) {
            // Should not be the same instance
            assertNotSame(firstClient.getConfiguration(), secondClient.getConfiguration());

            // Should not be affected by changes to firstClient's Configuration
            assertNull(secondClient.getConfiguration().getProperty("foo"));
            assertFalse(secondClient.getConfiguration().getClasses().contains(Always500.class));
        }
    }

    public static class Always500 implements ClientRequestFilter {

        @Override
        public void filter(ClientRequestContext requestContext) throws IOException {
            requestContext.abortWith(Response.serverError().build());
        }
    }
}
