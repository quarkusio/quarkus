package io.quarkus.smallrye.faulttolerance.test.config;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Retry;

@ApplicationScoped
public class RetryConfigBean {
    @Retry(delay = 0, jitter = 0)
    public void maxRetries(AtomicInteger counter) {
        counter.getAndIncrement();
        throw new TestException();
    }

    @Retry(maxDuration = 10000, durationUnit = ChronoUnit.MILLIS, maxRetries = 10000, delay = 200, jitter = 0)
    public void maxDuration() {
        throw new TestException();
    }

    @Retry(maxRetries = 5, delay = 2, delayUnit = ChronoUnit.SECONDS, jitter = 0)
    public void delay() {
        throw new TestException();
    }

    @Retry(maxRetries = 1, delay = 0, jitter = 0)
    public void retryOn(RuntimeException e, AtomicInteger counter) {
        counter.getAndIncrement();
        throw e;
    }

    @Retry(retryOn = { TestConfigExceptionA.class,
            TestConfigExceptionB.class }, abortOn = RuntimeException.class, maxRetries = 1, delay = 0, jitter = 0)
    public void abortOn(RuntimeException e, AtomicInteger counter) {
        counter.getAndIncrement();
        throw e;
    }

    private long lastStartTime = 0;

    @Retry(abortOn = TestConfigExceptionA.class, delay = 0, jitter = 0, maxRetries = 1000, maxDuration = 10, durationUnit = ChronoUnit.SECONDS)
    public void jitter() {
        long startTime = System.nanoTime();
        if (lastStartTime != 0) {
            Duration delay = Duration.ofNanos(startTime - lastStartTime);
            if (delay.compareTo(Duration.ofMillis(100)) > 0) {
                throw new TestConfigExceptionA();
            }
        }
        lastStartTime = startTime;
        throw new TestException();
    }
}
