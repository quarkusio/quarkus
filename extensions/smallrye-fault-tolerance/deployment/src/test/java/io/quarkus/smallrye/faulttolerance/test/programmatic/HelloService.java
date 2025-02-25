package io.quarkus.smallrye.faulttolerance.test.programmatic;

import java.time.temporal.ChronoUnit;
import java.util.function.Supplier;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;

import io.smallrye.faulttolerance.api.CircuitBreakerName;
import io.smallrye.faulttolerance.api.TypedGuard;

@ApplicationScoped
public class HelloService {
    static final String OK = "Hello";
    static final int THRESHOLD = 5;
    static final int DELAY = 500;

    private final Supplier<String> anotherHello = TypedGuard.create(String.class)
            .withCircuitBreaker().requestVolumeThreshold(THRESHOLD).delay(DELAY, ChronoUnit.MILLIS).name("another-hello").done()
            .build()
            .adaptSupplier(this::anotherHelloImpl);

    @CircuitBreaker(requestVolumeThreshold = THRESHOLD, delay = DELAY)
    @CircuitBreakerName("hello")
    public String hello(Exception exception) throws Exception {
        if (exception != null) {
            throw exception;
        }

        return OK;
    }

    public String anotherHello() {
        return anotherHello.get();
    }

    private String anotherHelloImpl() {
        throw new RuntimeException();
    }
}
