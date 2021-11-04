package io.quarkus.smallrye.faulttolerance.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.smallrye.faulttolerance.test.asynchronous.AsynchronousBean;
import io.quarkus.smallrye.faulttolerance.test.bukhead.BulkheadBean;
import io.quarkus.smallrye.faulttolerance.test.circuitbreaker.CircuitBreakerBean;
import io.quarkus.smallrye.faulttolerance.test.fallback.FallbackBean;
import io.quarkus.smallrye.faulttolerance.test.retry.RetryBean;
import io.quarkus.smallrye.faulttolerance.test.timeout.TimeoutBean;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.faulttolerance.api.CircuitBreakerMaintenance;
import io.smallrye.faulttolerance.api.CircuitBreakerState;

public class FaultToleranceTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(FallbackBean.class, BulkheadBean.class, TimeoutBean.class, RetryBean.class,
                            CircuitBreakerBean.class, AsynchronousBean.class));

    @Inject
    FallbackBean fallback;
    @Inject
    BulkheadBean bulkhead;
    @Inject
    TimeoutBean timeout;
    @Inject
    RetryBean retry;
    @Inject
    CircuitBreakerBean circuitbreaker;
    @Inject
    CircuitBreakerMaintenance circuitBreakerMaintenance;
    @Inject
    AsynchronousBean asynchronous;

    @Test
    public void testFallback() {
        assertEquals(FallbackBean.RecoverFallback.class.getName(), fallback.ping());
    }

    @Test
    public void testBulkhead() throws InterruptedException {
        AtomicBoolean success = new AtomicBoolean(true);
        AtomicBoolean bulkheadFailures = new AtomicBoolean(false);
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        for (int i = 0; i < 20; i++) {
            executorService.submit(() -> {
                try {
                    int bulk = bulkhead.bulkhead();
                    if (bulk > 5) {
                        success.set(false);
                    }
                } catch (BulkheadException be) {
                    bulkheadFailures.set(true);
                }
            });
            Thread.sleep(1);//give some chance to the bulkhead to happens
        }
        executorService.shutdown();
        executorService.awaitTermination(5, TimeUnit.SECONDS);
        assertTrue(success.get());
        assertTrue(bulkheadFailures.get());
    }

    @Test
    public void testTimeout() throws InterruptedException {
        assertThrows(TimeoutException.class, () -> timeout.timeout());
    }

    @Test
    public void testRetry() {
        assertTrue(retry.retry());
    }

    @Test
    public void testCircuitBreaker() {
        circuitbreaker.breakCircuit();
        assertThrows(RuntimeException.class, () -> circuitbreaker.breakCircuit());
        assertThrows(RuntimeException.class, () -> circuitbreaker.breakCircuit());
        assertThrows(RuntimeException.class, () -> circuitbreaker.breakCircuit());
        assertThrows(RuntimeException.class, () -> circuitbreaker.breakCircuit());
        assertThrows(CircuitBreakerOpenException.class, () -> circuitbreaker.breakCircuit());
        assertEquals(CircuitBreakerState.OPEN, circuitBreakerMaintenance.currentState("my-cb"));
    }

    @Test
    public void testAsynchronous() throws ExecutionException, InterruptedException {
        assertEquals("hello", asynchronous.asynchronous().toCompletableFuture().get());
    }

    @Test
    public void undefinedCircuitBreaker() {
        assertThrows(IllegalArgumentException.class, () -> {
            circuitBreakerMaintenance.currentState("undefined");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            circuitBreakerMaintenance.reset("undefined");
        });
    }

}
