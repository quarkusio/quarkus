package io.quarkus.micrometer.test;

import static java.util.concurrent.CompletableFuture.supplyAsync;

import java.util.concurrent.CompletableFuture;

import jakarta.enterprise.context.ApplicationScoped;

import io.micrometer.core.annotation.Counted;
import io.smallrye.mutiny.Uni;

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

    @Counted(value = "uni.none", recordFailuresOnly = true)
    public Uni<?> onlyCountUniFailures(GuardedResult guardedResult) {
        return Uni.createFrom().item(guardedResult::get);
    }

    @Counted(value = "uni.all", extraTags = { "extra", "tag" })
    public Uni<?> countAllUniInvocations(GuardedResult guardedResult) {
        return Uni.createFrom().item(guardedResult::get);
    }

    @Counted
    public Uni<?> emptyUniMetricName(GuardedResult guardedResult) {
        return Uni.createFrom().item(guardedResult::get);
    }

}
