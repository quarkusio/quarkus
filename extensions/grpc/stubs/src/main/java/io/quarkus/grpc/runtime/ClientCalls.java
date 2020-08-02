package io.quarkus.grpc.runtime;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import io.grpc.stub.StreamObserver;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.MultiEmitter;
import io.smallrye.mutiny.subscription.UniEmitter;

public class ClientCalls {

    private ClientCalls() {
    }

    public static <I, O> Uni<O> oneToOne(I request, BiConsumer<I, StreamObserver<O>> delegate) {
        return Uni.createFrom().emitter(new Consumer<UniEmitter<? super O>>() { // NOSONAR
            @Override
            public void accept(UniEmitter<? super O> emitter) {
                delegate.accept(request, new UniStreamObserver<>(emitter));
            }
        });
    }

    public static <I, O> Multi<O> oneToMany(I request, BiConsumer<I, StreamObserver<O>> delegate) {
        return Multi.createFrom().emitter(new Consumer<MultiEmitter<? super O>>() { // NOSONAR
            @Override
            public void accept(MultiEmitter<? super O> emitter) {
                delegate.accept(request, new MultiStreamObserver<>(emitter));
            }
        });
    }

    public static <I, O> Uni<O> manyToOne(Multi<I> items, Function<StreamObserver<O>, StreamObserver<I>> delegate) {
        return Uni.createFrom().emitter((new Consumer<UniEmitter<? super O>>() { // NOSONAR
            @Override
            public void accept(UniEmitter<? super O> emitter) {
                StreamObserver<I> request = delegate.apply(new UniStreamObserver<>(emitter));
                items.subscribe().with(
                        new Consumer<I>() {
                            @Override
                            public void accept(I v) {
                                request.onNext(v);
                            }
                        },
                        new Consumer<Throwable>() {
                            @Override
                            public void accept(Throwable throwable) {
                                request.onError(throwable);
                            }
                        },
                        new Runnable() {
                            @Override
                            public void run() {
                                request.onCompleted();
                            }
                        });
            }
        }));
    }

    public static <I, O> Multi<O> manyToMany(Multi<I> items, Function<StreamObserver<O>, StreamObserver<I>> delegate) {
        return Multi.createFrom().emitter((new Consumer<MultiEmitter<? super O>>() { // NOSONAR
            @Override
            public void accept(MultiEmitter<? super O> emitter) {
                StreamObserver<I> request = delegate.apply(new MultiStreamObserver<>(emitter));
                items.subscribe().with(
                        new Consumer<I>() {
                            @Override
                            public void accept(I v) {
                                request.onNext(v);
                            }
                        },
                        new Consumer<Throwable>() {
                            @Override
                            public void accept(Throwable throwable) {
                                request.onError(throwable);
                            }
                        },
                        new Runnable() {
                            @Override
                            public void run() {
                                request.onCompleted();
                            }
                        });
            }
        }));

    }
}
