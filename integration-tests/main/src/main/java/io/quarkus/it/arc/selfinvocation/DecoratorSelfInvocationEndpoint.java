package io.quarkus.it.arc.selfinvocation;

import java.time.Instant;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * Drives the reproducer and reports how many times the concrete handler ran. Lives here (not in the
 * test) so it is part of the real application used by the integration test and the native image.
 */
@Path("/decorator-self-invocation")
public class DecoratorSelfInvocationEndpoint {

    @Inject
    EnvelopeHandler<String> handler;

    @Inject
    GreetingHandler greetingHandler;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public int process() {
        int before = greetingHandler.getInvocations();
        handler.process(new TestEnvelope("hello", Instant.EPOCH, "test"));
        return greetingHandler.getInvocations() - before;
    }
}
