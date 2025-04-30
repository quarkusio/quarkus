package io.quarkus.smallrye.faulttolerance.test.fallback.causechain;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Fallback;

@ApplicationScoped
public class FallbackWithSkipOn {
    @Fallback(fallbackMethod = "fallback", skipOn = ExpectedOutcomeException.class)
    public void hello(Exception e) throws Exception {
        throw e;
    }

    public void fallback(Exception ignored) {
    }
}
