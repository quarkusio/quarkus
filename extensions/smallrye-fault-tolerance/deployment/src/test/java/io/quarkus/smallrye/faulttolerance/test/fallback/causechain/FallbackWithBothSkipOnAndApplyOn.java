package io.quarkus.smallrye.faulttolerance.test.fallback.causechain;

import java.io.IOException;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Fallback;

@ApplicationScoped
public class FallbackWithBothSkipOnAndApplyOn {
    @Fallback(fallbackMethod = "fallback", skipOn = ExpectedOutcomeException.class, applyOn = IOException.class)
    public void hello(Exception e) throws Exception {
        throw e;
    }

    public void fallback(Exception ignored) {
    }
}
