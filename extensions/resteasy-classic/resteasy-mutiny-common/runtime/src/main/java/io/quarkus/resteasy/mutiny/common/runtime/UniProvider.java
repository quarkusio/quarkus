package io.quarkus.resteasy.mutiny.common.runtime;

import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

import org.jboss.resteasy.spi.AsyncClientResponseProvider;
import org.jboss.resteasy.spi.AsyncResponseProvider;

import io.smallrye.mutiny.Uni;

public class UniProvider implements AsyncResponseProvider<Uni<?>>, AsyncClientResponseProvider<Uni<?>> {

    @Override
    public CompletionStage<?> toCompletionStage(Uni<?> uni) {
        return uni.subscribeAsCompletionStage();
    }

    @Override
    public Uni<?> fromCompletionStage(CompletionStage<?> completionStage) {
        return Uni.createFrom().completionStage(completionStage);
    }

    @Override
    public Uni<?> fromCompletionStage(Supplier<CompletionStage<?>> supplier) {
        return Uni.createFrom().completionStage(supplier);
    }

}