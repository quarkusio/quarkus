package io.quarkus.smallrye.faulttolerance.test.hotreload;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Fallback;

@ApplicationScoped
public class HotReloadBean {
    @Fallback(fallbackMethod = "fallback1")
    public String hello() {
        throw new RuntimeException();
    }

    public String fallback1() {
        return "fallback1";
    }

    public String fallback2() {
        return "fallback2";
    }
}
