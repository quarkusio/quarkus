package io.quarkus.smallrye.faulttolerance.test.circuitbreaker;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;

@ApplicationScoped
public class CircuitBreakerBean {
    AtomicBoolean breakTheCircuit = new AtomicBoolean();

    @CircuitBreaker(requestVolumeThreshold = 5)
    public void breakCircuit() {
        if (!breakTheCircuit.getAndSet(true)) {
            return;
        }
        throw new RuntimeException("let's break it !");
    }
}
