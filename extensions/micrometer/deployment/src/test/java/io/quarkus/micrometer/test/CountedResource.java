package io.quarkus.micrometer.test;

import static java.util.concurrent.CompletableFuture.supplyAsync;

import java.util.concurrent.CompletableFuture;

import javax.enterprise.context.ApplicationScoped;

import io.micrometer.core.annotation.Counted;

@ApplicationScoped
public class CountedResource {
    @Counted(value = "metric.none", recordFailuresOnly = true)
    public void onlyCountFailures() {
    }

    @Counted(value = "metric.all", extraTags = { "extra", "tag" })
    public void countAllInvocations(boolean fail) {
        if (fail) {
            throw new NullPointerException("Failed on purpose");
        }
    }

    @Counted(description = "nice description")
    public void emptyMetricName(boolean fail) {
        if (fail) {
            throw new NullPointerException("Failed on purpose");
        }
    }

    @Counted(value = "async.none", recordFailuresOnly = true)
    public CompletableFuture<?> onlyCountAsyncFailures(GuardedResult guardedResult) {
        return supplyAsync(guardedResult::get);
    }

    @Counted(value = "async.all", extraTags = { "extra", "tag" })
    public CompletableFuture<?> countAllAsyncInvocations(GuardedResult guardedResult) {
        return supplyAsync(guardedResult::get);
    }

    @Counted
    public CompletableFuture<?> emptyAsyncMetricName(GuardedResult guardedResult) {
        return supplyAsync(guardedResult::get);
    }

}
