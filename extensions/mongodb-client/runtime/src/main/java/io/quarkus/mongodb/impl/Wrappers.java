package io.quarkus.mongodb.impl;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.reactivestreams.Publisher;

import com.mongodb.reactivestreams.client.Success;

import io.reactivex.Flowable;
import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.reactivex.RxHelper;

class Wrappers {
    private static final Supplier<RuntimeException> UNEXPECTED_EMPTY_STREAM = () -> new IllegalStateException(
            "Unexpected empty stream");

    private Wrappers() {
        // Avoid direct instantiation.
    }

    static <T> CompletionStage<T> toCompletionStage(Publisher<T> publisher) {
        Context context = Vertx.currentContext();
        CompletionStage<Optional<T>> run = ReactiveStreams.fromPublisher(publisher)
                .findFirst()
                .run();
        CompletableFuture<T> cf = new CompletableFuture<>();
        run.whenComplete((opt, err) -> {
            if (context != null) {
                context.runOnContext(x -> completeOrFailedTheFuture(cf, opt, err));
            } else {
                completeOrFailedTheFuture(cf, opt, err);
            }
        });
        return cf;

    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private static <T> void completeOrFailedTheFuture(CompletableFuture<T> cf, Optional<T> opt, Throwable err) {
        if (err != null) {
            cf.completeExceptionally(err);
        } else {
            cf.complete(opt.orElseThrow(UNEXPECTED_EMPTY_STREAM));
        }
    }

    static <T> CompletionStage<List<T>> toCompletionStageOfList(Publisher<T> publisher) {
        @Nullable
        Context context = Vertx.currentContext();
        CompletionStage<List<T>> run = ReactiveStreams.fromPublisher(publisher)
                .toList()
                .run();
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

    static CompletionStage<Void> toEmptyCompletionStage(Publisher<Success> publisher) {
        return toCompletionStage(publisher).thenApply(x -> null);
    }

    static <T> PublisherBuilder<T> toPublisherBuilder(Publisher<T> publisher) {
        @Nullable
        Context context = Vertx.currentContext();
        if (context != null) {
            Flowable<T> flowable = Flowable.fromPublisher(publisher);
            return ReactiveStreams.fromPublisher(flowable.observeOn(RxHelper.scheduler(context)));
        } else {
            return ReactiveStreams.fromPublisher(publisher);
        }

    }
}
