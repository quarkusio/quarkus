package io.quarkus.micrometer.test;

import static java.util.concurrent.CompletableFuture.supplyAsync;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;

import jakarta.enterprise.context.ApplicationScoped;

import io.micrometer.core.annotation.Timed;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class TimedResource {
    @Timed(value = "call", extraTags = { "extra", "tag" })
    public void call(boolean fail) {
        if (fail) {
            throw new NullPointerException("Failed on purpose");
        }

    }

    @Timed(value = "longCall", extraTags = { "extra", "tag" }, longTask = true)
    public void longCall(boolean fail) {
        try {
            Thread.sleep(3);
        } catch (InterruptedException e) {
        }
        if (fail) {
            throw new NullPointerException("Failed on purpose");
        }
    }

    @Timed(value = "async.call", extraTags = { "extra", "tag" })
    public CompletableFuture<?> asyncCall(GuardedResult guardedResult) {
        return supplyAsync(guardedResult::get);
    }

    @Timed(value = "uni.call", extraTags = { "extra", "tag" })
    public Uni<?> uniCall(GuardedResult guardedResult) {
        return Uni.createFrom().item(guardedResult::get);
    }

    @Timed(value = "async.longCall", extraTags = { "extra", "tag" }, longTask = true)
    public CompletableFuture<?> longAsyncCall(GuardedResult guardedResult) {
        try {
            Thread.sleep(3);
        } catch (InterruptedException e) {
        }
        return supplyAsync(guardedResult::get);
    }

    @Timed(value = "uni.longCall", extraTags = { "extra", "tag" }, longTask = true)
    public Uni<?> longUniCall(GuardedResult guardedResult) {
        return Uni.createFrom().item(guardedResult::get).onItem().delayIt().by(Duration.of(3, ChronoUnit.MILLIS));
    }

    @Timed(value = "alpha", extraTags = { "extra", "tag" })
    @Timed(value = "bravo", extraTags = { "extra", "tag" })
    public void repeatableCall(boolean fail) {
        if (fail) {
            throw new NullPointerException("Failed on purpose");
        }
    }
}
