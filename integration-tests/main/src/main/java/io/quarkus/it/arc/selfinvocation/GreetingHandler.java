package io.quarkus.it.arc.selfinvocation;

import java.time.Instant;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Top-level class (not nested in the test) so it is part of the real application bean archive,
 * which the integration test and the native image require.
 */
@ApplicationScoped
public class GreetingHandler extends AbstractEnvelopeHandler<String> {

    private int invocations;

    @Override
    public Class<String> payloadType() {
        return String.class;
    }

    @Override
    public void process(String payload, Instant timestamp) {
        invocations++;
    }

    public int getInvocations() {
        return invocations;
    }
}
