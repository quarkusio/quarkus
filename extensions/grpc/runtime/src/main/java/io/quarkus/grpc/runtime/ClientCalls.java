package io.quarkus.grpc.runtime;

import java.util.function.BiConsumer;
import java.util.function.Function;

import io.grpc.stub.StreamObserver;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public class ClientCalls {

    private ClientCalls() {
    }

    public static <I, O> Uni<O> oneToOne(I request, BiConsumer<I, StreamObserver<O>> delegate) {
        return Uni.createFrom().emitter(emitter -> delegate.accept(request, new UniStreamObserver<>(emitter)));
    }

    public static <I, O> Multi<O> oneToMany(I request, BiConsumer<I, StreamObserver<O>> delegate) {
        return Multi.createFrom().emitter(emitter -> delegate.accept(request, new MultiStreamObserver<>(emitter)));
    }

    public static <I, O> Uni<O> manyToOne(Multi<I> items, Function<StreamObserver<O>, StreamObserver<I>> delegate) {
        return Uni.createFrom().emitter((emitter -> {
            StreamObserver<I> request = delegate.apply(new UniStreamObserver<>(emitter));
            items.subscribe().with(
                    request::onNext,
                    request::onError,
                    request::onCompleted);
        }));
    }

    public static <I, O> Multi<O> manyToMany(Multi<I> items, Function<StreamObserver<O>, StreamObserver<I>> delegate) {
        return Multi.createFrom().emitter((emitter -> {
            StreamObserver<I> request = delegate.apply(new MultiStreamObserver<>(emitter));
            items.subscribe().with(
                    request::onNext,
                    request::onError,
                    request::onCompleted);
        }));

    }
}
