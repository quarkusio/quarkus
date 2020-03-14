package io.quarkus.mongodb.impl;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.reactivestreams.Publisher;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

class Wrappers {

    private Wrappers() {
        // Avoid direct instantiation.
    }

    static <T> Uni<T> toUni(Publisher<T> publisher) {
        Context context = Vertx.currentContext();
        Uni<T> uni = Uni.createFrom().publisher(publisher);
        if (context != null) {
            return uni.emitOn(command -> context.runOnContext(x -> command.run()));
        }
        return uni;
    }

    static <T> Multi<T> toMulti(Publisher<T> publisher) {
        @Nullable
        Context context = Vertx.currentContext();
        if (context != null) {
            return Multi.createFrom().publisher(publisher).emitOn(command -> context.runOnContext(x -> command.run()));
        } else {
            return Multi.createFrom().publisher(publisher);
        }
    }

    static <T> Uni<List<T>> toUniOfList(Publisher<T> publisher) {
        @Nullable
        Context context = Vertx.currentContext();
        Uni<List<T>> uni = Multi.createFrom().publisher(publisher)
                .collectItems().asList();

        if (context != null) {
            return uni.emitOn(command -> context.runOnContext(x -> command.run()));
        }
        return uni;
    }

    static <T> PublisherBuilder<T> toPublisherBuilder(Publisher<T> publisher) {
        @Nullable
        Context context = Vertx.currentContext();
        if (context != null) {
            Multi<T> multi = Multi.createFrom().publisher(publisher)
                    .emitOn(command -> context.runOnContext(x -> command.run()));
            return ReactiveStreams.fromPublisher(multi);
        } else {
            return ReactiveStreams.fromPublisher(publisher);
        }
    }

    public static <T> CompletionStage<T> toCompletionStage(Publisher<T> publisher) {
        Context context = Vertx.currentContext();
        CompletableFuture<T> future = Multi.createFrom().publisher(publisher)
                .collectItems().first()
                .subscribeAsCompletionStage();

        CompletableFuture<T> result = new CompletableFuture<>();
        future.whenComplete((value, err) -> {
            if (context != null) {
                context.runOnContext(x -> completeOrFailedTheFuture(result, value, err));
            } else {
                completeOrFailedTheFuture(result, value, err);
            }
        });
        return result;
    }

    private static <T> void completeOrFailedTheFuture(CompletableFuture<T> cf, T value, Throwable err) {
        if (err != null) {
            cf.completeExceptionally(err);
        } else {
            cf.complete(value);
        }
    }

    static <T> CompletionStage<List<T>> toCompletionStageOfList(Publisher<T> publisher) {
        @Nullable
        Context context = Vertx.currentContext();
        CompletionStage<List<T>> run = Multi.createFrom().publisher(publisher)
                .collectItems().asList()
                .subscribeAsCompletionStage();
        CompletableFuture<List<T>> cf = new CompletableFuture<>();
        run.thenAccept(list -> {
            if (context != null) {
                context.runOnContext(x -> cf.complete(list));
            } else {
                cf.complete(list);
            }
        });
        return cf;

    }

}
