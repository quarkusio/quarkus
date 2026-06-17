package io.quarkus.grpc.stubs;

import java.util.Queue;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jctools.queues.atomic.unpadded.SpscChunkedAtomicUnpaddedArrayQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.grpc.stub.ClientCallStreamObserver;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import io.smallrye.mutiny.Context;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.subscription.MultiEmitter;
import io.smallrye.mutiny.subscription.Subscribers;
import io.smallrye.mutiny.subscription.UniEmitter;

public class ClientCalls {

    final static long PREFETCH = 256;
    final static long REPLENISH = PREFETCH * 3 / 4;

    final static String ERROR_CAST_STREAM_OBSERVER = String.format("%s can't be casted as a %s", StreamObserver.class,
            ServerCallStreamObserver.class);
    private static final Logger log = LoggerFactory.getLogger(ClientCalls.class);

    private ClientCalls() {
    }

    public static <I, O> Uni<O> oneToOne(I request, BiConsumer<I, StreamObserver<O>> delegate) {
        return Uni.createFrom().emitter(new Consumer<UniEmitter<? super O>>() { // NOSONAR
            @Override
            public void accept(UniEmitter<? super O> emitter) {
                delegate.accept(request, new UniStreamObserver<>(emitter, null));
            }
        });
    }

    public static <I, O> Multi<O> oneToMany(I request, BiConsumer<I, StreamObserver<O>> delegate) {
        return Multi.createFrom().emitter(new Consumer<MultiEmitter<? super O>>() { // NOSONAR
            @Override
            public void accept(MultiEmitter<? super O> emitter) {
                delegate.accept(request, new MultiStreamObserver<>(emitter, null));
            }
        });
    }

    public static <I, O> Uni<O> manyToOne(Multi<I> items, Function<StreamObserver<O>, StreamObserver<I>> delegate) {
        return Uni.createFrom().emitter((new Consumer<UniEmitter<? super O>>() {
            @Override
            public void accept(UniEmitter<? super O> emitter) {
                AtomicReference<Flow.Subscription> cancellable = new AtomicReference<>();
                AtomicReference<Runnable> drainRef = new AtomicReference<>();
                Runnable onReady = () -> {
                    Runnable drain = drainRef.get();
                    if (drain != null) {
                        drain.run();
                    }
                };
                UniStreamObserver<I, O> responseObserver = new UniStreamObserver<>(emitter, () -> {
                    var subscription = cancellable.getAndSet(Subscriptions.CANCELLED);
                    if (subscription != null) {
                        subscription.cancel();
                    }
                }, onReady);

                StreamObserver<I> request = delegate.apply(responseObserver);

                if (request instanceof ClientCallStreamObserver<I>) {
                    drainLoop((ClientCallStreamObserver<I>) request, items, cancellable, drainRef);
                } else {
                    responseObserver.onError(new Throwable(ERROR_CAST_STREAM_OBSERVER));
                    log.error(ERROR_CAST_STREAM_OBSERVER);
                }
            }
        }));
    }

    public static <I, O> Multi<O> manyToMany(Multi<I> items, Function<StreamObserver<O>, StreamObserver<I>> delegate) {

        return Multi.createFrom().emitter((new Consumer<MultiEmitter<? super O>>() {

            @Override
            public void accept(MultiEmitter<? super O> emitter) {
                AtomicReference<Flow.Subscription> cancellable = new AtomicReference<>();
                AtomicReference<Runnable> drainRef = new AtomicReference<>();

                Runnable onReady = () -> {
                    Runnable drain = drainRef.get();
                    if (drain != null) {
                        drain.run();
                    }
                };
                MultiStreamObserver<I, O> responseObserver = new MultiStreamObserver<>(
                        emitter, () -> {
                            var subscription = cancellable.getAndSet(Subscriptions.CANCELLED);
                            if (subscription != null) {
                                subscription.cancel();
                            }
                        }, onReady);

                StreamObserver<I> request = delegate.apply(responseObserver);
                if (request instanceof ClientCallStreamObserver<I>) {
                    drainLoop((ClientCallStreamObserver<I>) request, items, cancellable, drainRef);
                } else {
                    responseObserver.onError(new Throwable(ERROR_CAST_STREAM_OBSERVER));
                    log.error(ERROR_CAST_STREAM_OBSERVER);
                }
            }

        }));
    }

    private static <I> void drainLoop(ClientCallStreamObserver<I> requestFlow, Multi<I> items,
            AtomicReference<Flow.Subscription> cancellable, AtomicReference<Runnable> drainRef) {
        Queue<I> queue = new SpscChunkedAtomicUnpaddedArrayQueue<>((int) PREFETCH, (int) PREFETCH * 2);
        AtomicBoolean done = new AtomicBoolean(false);
        AtomicInteger wip = new AtomicInteger(0);
        long[] consumed = { 0 };

        AtomicReference<Flow.Subscription> subscription = new AtomicReference<>();

        drainRef.set(() -> {
            if (subscription.get() == null || wip.getAndIncrement() != 0) {
                return;
            }
            try {
                do {
                    while (requestFlow.isReady()) {
                        I item = queue.poll();
                        if (item == null) {
                            if (done.get()) {
                                requestFlow.onCompleted();
                                return;
                            }
                            break;
                        }
                        requestFlow.onNext(item);
                        consumed[0]++;
                        if (consumed[0] >= REPLENISH) {
                            subscription.get().request(consumed[0]);
                            consumed[0] = 0;
                        }
                    }
                } while (wip.decrementAndGet() != 0);
            } catch (Throwable t) {
                requestFlow.onError(t);
            }
        });

        items.subscribe().withSubscriber(Subscribers.from(
                Context.empty(),
                item -> {
                    if (cancellable.get() != null && cancellable.get() != Subscriptions.CANCELLED) {
                        queue.offer(item);
                        drainRef.get().run();
                    }
                },
                requestFlow::onError,
                () -> {
                    done.set(true);
                    drainRef.get().run();
                },
                s -> {
                    if (!cancellable.compareAndSet(null, s)) {
                        s.cancel();
                    }
                    subscription.set(s);
                    s.request(PREFETCH);
                }));
    }

}
