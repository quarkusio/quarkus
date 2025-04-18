package io.quarkus.grpc.stubs;

import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import io.grpc.stub.StreamObserver;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.Subscriptions;
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
        return Uni.createFrom().emitter((new Consumer<UniEmitter<? super O>>() {
            @Override
            public void accept(UniEmitter<? super O> emitter) {
                AtomicReference<Flow.Subscription> cancellable = new AtomicReference<>();
                UniStreamObserver<O> observer = new UniStreamObserver<>(emitter.onTermination(() -> {
                    var subscription = cancellable.getAndSet(Subscriptions.CANCELLED);
                    if (subscription != null) {
                        subscription.cancel();
                    }
                }));
                StreamObserver<I> request = delegate.apply(observer);
                StreamObserverFlowHandler<I> flowHandler = new StreamObserverFlowHandler<>(request);
                subscribeToUpstreamAndForwardToStreamObserver(items, cancellable, flowHandler);
            }

        }));
    }

    private static <I> void subscribeToUpstreamAndForwardToStreamObserver(Multi<I> items,
            AtomicReference<Flow.Subscription> cancellable,
            StreamObserver<I> request) {
        items.subscribe().with(
                new Consumer<Flow.Subscription>() {
                    @Override
                    public void accept(Flow.Subscription subscription) {
                        if (!cancellable.compareAndSet(null, subscription)) {
                            subscription.cancel();
                        } else {
                            subscription.request(Long.MAX_VALUE);
                        }
                    }
                },

                new Consumer<I>() {
                    @Override
                    public void accept(I v) {
                        if (cancellable.get() != null && cancellable.get() != Subscriptions.CANCELLED) {
                            request.onNext(v);
                        }
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

    public static <I, O> Multi<O> manyToMany(Multi<I> items, Function<StreamObserver<O>, StreamObserver<I>> delegate) {
        return Multi.createFrom().emitter((new Consumer<MultiEmitter<? super O>>() {
            @Override
            public void accept(MultiEmitter<? super O> emitter) {
                AtomicReference<Flow.Subscription> cancellable = new AtomicReference<>();
                MultiStreamObserver<O> observer = new MultiStreamObserver<>(emitter.onTermination(() -> {
                    var subscription = cancellable.getAndSet(Subscriptions.CANCELLED);
                    if (subscription != null) {
                        subscription.cancel();
                    }
                }));
                StreamObserver<I> request = delegate.apply(observer);
                StreamObserverFlowHandler<I> flowHandler = new StreamObserverFlowHandler<>(request);
                subscribeToUpstreamAndForwardToStreamObserver(items, cancellable, flowHandler);
            }
        }));

    }
}
