package io.quarkus.smallrye.faulttolerance.test.config;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Timeout;

@ApplicationScoped
public class TimeoutConfigBean {
    @Timeout(value = 1, unit = ChronoUnit.MILLIS)
    public void value() throws InterruptedException {
        Thread.sleep(TimeUnit.MINUTES.toMillis(1));
    }

    @Timeout(value = 1000, unit = ChronoUnit.MICROS)
    public void unit() throws InterruptedException {
        Thread.sleep(TimeUnit.MINUTES.toMillis(1));
    }

    @Timeout(value = 10, unit = ChronoUnit.MICROS)
    @Asynchronous
    public CompletionStage<Void> both() throws InterruptedException {
        Thread.sleep(TimeUnit.MINUTES.toMillis(1));
        return CompletableFuture.completedFuture(null);
    }
}
