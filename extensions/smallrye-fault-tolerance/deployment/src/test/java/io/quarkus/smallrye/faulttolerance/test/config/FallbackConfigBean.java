package io.quarkus.smallrye.faulttolerance.test.config;

import jakarta.enterprise.context.Dependent;

import org.eclipse.microprofile.faulttolerance.Fallback;

@Dependent
public class FallbackConfigBean {
    @Fallback(fallbackMethod = "theFallback", applyOn = TestConfigExceptionB.class)
    public String applyOn() {
        throw new TestConfigExceptionA();
    }

    @Fallback(fallbackMethod = "theFallback")
    public String skipOn() {
        throw new TestConfigExceptionA();
    }

    @Fallback(fallbackMethod = "theFallback")
    public String fallbackMethod() {
        throw new IllegalArgumentException();
    }

    @Fallback(FallbackHandlerA.class)
    public String fallbackHandler() {
        throw new IllegalArgumentException();
    }

    public String theFallback() {
        return "FALLBACK";
    }

    public String anotherFallback() {
        return "ANOTHER FALLBACK";
    }
}
