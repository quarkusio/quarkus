package io.quarkus.smallrye.faulttolerance.test.reuse.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
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

public class ConfigReuseCircuitBreakerTest {
    private static final int NEW_THRESHOLD = 5;
    private static final int NEW_DELAY = 500;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(HelloService.class, MyGuard.class))
            .overrideConfigKey("quarkus.fault-tolerance.\"my-guard\".circuit-breaker.request-volume-threshold",
                    "" + NEW_THRESHOLD)
            .overrideConfigKey("quarkus.fault-tolerance.\"my-guard\".circuit-breaker.delay", "" + NEW_DELAY);

    @Inject
    HelloService helloService;

    @Inject
    CircuitBreakerMaintenance cb;

    @BeforeEach
    public void reset() {
        CircuitBreakerMaintenance.get().resetAll();
    }

    @Test
    public void test() {
        // force guard instantiation
        assertThatCode(() -> {
            helloService.hello(null);
        }).doesNotThrowAnyException();

        CircuitBreakerMaintenance cbm = CircuitBreakerMaintenance.get();

        assertThat(cb.currentState(MyGuard.NAME)).isEqualTo(CircuitBreakerState.CLOSED);
        assertThat(cbm.currentState(MyGuard.NAME)).isEqualTo(CircuitBreakerState.CLOSED);

        AtomicInteger helloStateChanges = new AtomicInteger();

        cbm.onStateChange(MyGuard.NAME, ignored -> {
            helloStateChanges.incrementAndGet();
        });

        for (int i = 0; i < NEW_THRESHOLD - 1; i++) { // `- 1` because of the initial invocation above
            assertThatThrownBy(() -> {
                helloService.hello(new IOException());
            }).isExactlyInstanceOf(IOException.class);
        }

        assertThat(cb.currentState(MyGuard.NAME)).isEqualTo(CircuitBreakerState.OPEN);
        assertThat(cbm.currentState(MyGuard.NAME)).isEqualTo(CircuitBreakerState.OPEN);

        // 1. closed -> open
        assertThat(helloStateChanges).hasValue(1);

        await().atMost(NEW_DELAY * 2, TimeUnit.MILLISECONDS)
                .ignoreException(CircuitBreakerOpenException.class)
                .untilAsserted(() -> {
                    assertThat(helloService.hello(null)).isEqualTo(HelloService.OK);
                });

        assertThat(cb.currentState(MyGuard.NAME)).isEqualTo(CircuitBreakerState.CLOSED);
        assertThat(cbm.currentState(MyGuard.NAME)).isEqualTo(CircuitBreakerState.CLOSED);

        // 2. open -> half-open
        // 3. half-open -> closed
        assertThat(helloStateChanges).hasValue(3);
    }
}
