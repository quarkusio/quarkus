package io.quarkus.smallrye.faulttolerance.test.programmatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.inject.Inject;

import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.faulttolerance.api.CircuitBreakerMaintenance;
import io.smallrye.faulttolerance.api.CircuitBreakerState;
import io.smallrye.faulttolerance.api.FaultTolerance;

public class ProgrammaticCircuitBreakerTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(HelloService.class));

    @Inject
    HelloService helloService;

    @Inject
    CircuitBreakerMaintenance cb;

    @BeforeEach
    public void reset() {
        FaultTolerance.circuitBreakerMaintenance().resetAll();

        helloService.toString(); // force bean instantiation
    }

    @Test
    public void test() {
        CircuitBreakerMaintenance cbm = FaultTolerance.circuitBreakerMaintenance();

        assertThat(cb.currentState("hello")).isEqualTo(CircuitBreakerState.CLOSED);
        assertThat(cb.currentState("another-hello")).isEqualTo(CircuitBreakerState.CLOSED);
        assertThat(cbm.currentState("hello")).isEqualTo(CircuitBreakerState.CLOSED);
        assertThat(cbm.currentState("another-hello")).isEqualTo(CircuitBreakerState.CLOSED);

        AtomicInteger helloStateChanges = new AtomicInteger();
        AtomicInteger anotherHelloStateChanges = new AtomicInteger();

        cbm.onStateChange("hello", ignored -> {
            helloStateChanges.incrementAndGet();
        });
        cb.onStateChange("another-hello", ignored -> {
            anotherHelloStateChanges.incrementAndGet();
        });

        for (int i = 0; i < HelloService.THRESHOLD; i++) {
            assertThatThrownBy(() -> {
                helloService.hello(new IOException());
            }).isExactlyInstanceOf(IOException.class);

            assertThatThrownBy(() -> {
                helloService.anotherHello();
            }).isExactlyInstanceOf(RuntimeException.class);
        }

        assertThat(cb.currentState("hello")).isEqualTo(CircuitBreakerState.OPEN);
        assertThat(cb.currentState("another-hello")).isEqualTo(CircuitBreakerState.OPEN);
        assertThat(cbm.currentState("hello")).isEqualTo(CircuitBreakerState.OPEN);
        assertThat(cbm.currentState("another-hello")).isEqualTo(CircuitBreakerState.OPEN);

        // hello 1. closed -> open
        assertThat(helloStateChanges).hasValue(1);
        // another-hello 1. closed -> open
        assertThat(anotherHelloStateChanges).hasValue(1);

        await().atMost(HelloService.DELAY * 2, TimeUnit.MILLISECONDS)
                .ignoreException(CircuitBreakerOpenException.class)
                .untilAsserted(() -> {
                    assertThat(helloService.hello(null)).isEqualTo(HelloService.OK);
                });
        await().atMost(HelloService.DELAY * 2, TimeUnit.MILLISECONDS)
                .ignoreException(CircuitBreakerOpenException.class)
                .untilAsserted(() -> {
                    assertThatThrownBy(helloService::anotherHello).isExactlyInstanceOf(RuntimeException.class);
                });

        assertThat(cb.currentState("hello")).isEqualTo(CircuitBreakerState.CLOSED);
        assertThat(cb.currentState("another-hello")).isEqualTo(CircuitBreakerState.OPEN);
        assertThat(cbm.currentState("hello")).isEqualTo(CircuitBreakerState.CLOSED);
        assertThat(cbm.currentState("another-hello")).isEqualTo(CircuitBreakerState.OPEN);

        // hello 2. open -> half-open
        // hello 3. half-open -> closed
        assertThat(helloStateChanges).hasValue(3);
        // another-hello 2. open -> half-open
        // another-hello 3. half-open -> open
        assertThat(anotherHelloStateChanges).hasValue(3);
    }
}
