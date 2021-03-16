package io.quarkus.smallrye.faulttolerance.test.circuitbreaker;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;

import io.smallrye.faulttolerance.api.CircuitBreakerState;
import io.smallrye.faulttolerance.api.CircuitBreakerStateChanged;

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

    @ApplicationScoped
    public static class Observer {
        private volatile boolean isOpen = false;

        public void onStateChange(@Observes CircuitBreakerStateChanged event) {
            isOpen = event.targetState == CircuitBreakerState.OPEN;
        }

        public boolean isOpen() {
            return isOpen;
        }
    }
}
