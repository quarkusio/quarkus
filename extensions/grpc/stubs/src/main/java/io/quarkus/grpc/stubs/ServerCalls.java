package io.quarkus.grpc.stubs;

import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.jboss.logging.Logger;
import org.jctools.queues.SpscChunkedArrayQueue;

import io.grpc.Status;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import io.quarkus.arc.Arc;
import io.quarkus.grpc.ExceptionHandlerProvider;
import io.smallrye.mutiny.Context;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.operators.multi.processors.UnicastProcessor;
import io.smallrye.mutiny.subscription.Subscribers;

public class ServerCalls {

    final static long INITIAL_BATCH = 100;
    final static long REQUEST_BATCH = 1;

    final static String ERROR_CAST_STREAM_OBSERVER = String.format("%s can't be casted as a %s", StreamObserver.class,
            ServerCallStreamObserver.class);

    private static final Logger log = Logger.getLogger(ServerCalls.class);

    private static StreamCollector streamCollector = StreamCollector.NO_OP;

    private ServerCalls() {
    }

    public static <I, O> void oneToOne(I request, StreamObserver<O> response, String compression,
            Function<I, Uni<O>> implementation) {
        trySetCompression(response, compression);
        streamCollector.add(response);
        try {
            Uni<O> uni = implementation.apply(request);
            if (uni == null) {
                log.error("gRPC service method returned null instead of Uni. " +
                        "Please change the implementation to return a Uni object, either carrying a value or a failure," +
                        " or throw StatusRuntimeException");
                onError(response, Status.fromCode(Status.Code.INTERNAL).asException());
                return;
            }
            uni.subscribe().with(
                    item -> {
                        response.onNext(item);
                        onCompleted(response);
                    },
                    failure -> onError(response, failure));
        } catch (Throwable t) {
            onError(response, t);
        }
    }

    public static <I, O> void oneToMany(I request, StreamObserver<O> response, String compression,
            Function<I, Multi<O>> implementation) {
        if (response instanceof ServerCallStreamObserver<O>) {
            ServerCallStreamObserver<O> responseFlowControl = (ServerCallStreamObserver<O>) response;
            try {
                trySetCompression(responseFlowControl, compression);
                streamCollector.add(responseFlowControl);
                var multi = implementation.apply(request);

                handleSubscription(multi, responseFlowControl);
            } catch (Throwable throwable) {
                onError(responseFlowControl, throwable);
            }
        } else {
            onError(response, new Throwable(ERROR_CAST_STREAM_OBSERVER));
            log.error(ERROR_CAST_STREAM_OBSERVER);
        }
    }

    public static <I, O> StreamObserver<I> manyToOne(StreamObserver<O> response,
            Function<Multi<I>, Uni<O>> implementation) {
        try {
            UnicastProcessor<I> input = UnicastProcessor.create();
            StreamObserver<I> pump = getStreamObserverFeedingProcessor(input);
            streamCollector.add(response);

            Uni<O> uni = implementation.apply(input);
            if (uni == null) {
                log.error("gRPC service method returned null instead of Uni. " +
                        "Please change the implementation to return a Uni object, either carrying a value or a failure," +
                        " or throw StatusRuntimeException");
                onError(response, Status.fromCode(Status.Code.INTERNAL).asException());
                return null;
            }
            uni.subscribe().with(
                    item -> {
                        response.onNext(item);
                        onCompleted(response);
                    },
                    failure -> onError(response, failure));
            return pump;
        } catch (Throwable throwable) {
            onError(response, throwable);
            return null;
        }
    }

    public static <I, O> StreamObserver<I> manyToMany(StreamObserver<O> response,
            Function<Multi<I>, Multi<O>> implementation) {
        if (response instanceof ServerCallStreamObserver<O>) {
            ServerCallStreamObserver<O> responseFlowControl = (ServerCallStreamObserver<O>) response;
            try {
                streamCollector.add(responseFlowControl);
                UnicastProcessor<I> input = UnicastProcessor.create();
                StreamObserver<I> pump = getStreamObserverFeedingProcessor(input);
                Multi<O> multi = implementation.apply(input);
                if (multi == null) {
                    log.error("gRPC service method returned null instead of Multi. " +
                            "Please change the implementation to return a Multi object or throw StatusRuntimeException");
                    onError(responseFlowControl, Status.fromCode(Status.Code.INTERNAL).asException());
                    return null;
                }

                handleSubscription(multi, responseFlowControl);
                return pump;
            } catch (Throwable throwable) {
                onError(responseFlowControl, throwable);
                return null;
            }
        } else {
            log.error(ERROR_CAST_STREAM_OBSERVER);
            onError(response, new Throwable(ERROR_CAST_STREAM_OBSERVER));
            return null;
        }
    }

    public static <O> void handleSubscription(Multi<O> items, ServerCallStreamObserver<O> response) {
        SpscChunkedArrayQueue<O> queue = new SpscChunkedArrayQueue<>(64, 256);
        AtomicBoolean done = new AtomicBoolean(false);
        AtomicInteger wip = new AtomicInteger(0);

        AtomicReference<Flow.Subscription> subscription = new AtomicReference<>();

        Runnable drain = new Runnable() {
            @Override
            public void run() {
                if (wip.getAndIncrement() != 0) {
                    return;
                }
                try {
                    do {
                        while (response.isReady()) {
                            O item = queue.poll();
                            if (item == null) {
                                if (done.get()) {
                                    response.onCompleted();
                                    return;
                                }
                                break;
                            }
                            response.onNext(item);
                            subscription.get().request(REQUEST_BATCH);
                        }
                    } while (wip.decrementAndGet() != 0);
                } catch (Throwable t) {
                    onError(response, t);
                }
            }
        };

        var cancellable = items.subscribe().withSubscriber(Subscribers.from(
                Context.empty(),
                item -> {
                    queue.offer(item);
                    drain.run();
                },
                failure -> onError(response, failure),
                () -> {
                    done.set(true);
                    drain.run();
                },
                s -> {
                    subscription.set(s);
                    s.request(INITIAL_BATCH);
                }));

        response.setOnReadyHandler(drain);
        response.setOnCloseHandler(cancellable::cancel);
        response.setOnCancelHandler(cancellable::cancel);
        drain.run();
    }

    private static <O> void onCompleted(StreamObserver<O> response) {
        try {
            response.onCompleted();
        } finally {
            streamCollector.remove(response);
        }
    }

    private static ExceptionHandlerProvider ehp;

    private static ExceptionHandlerProvider getEhp() {
        if (ehp == null) {
            ehp = Arc.container().select(ExceptionHandlerProvider.class).get();
        }
        return ehp;
    }

    private static <O> void onError(StreamObserver<O> response, Throwable error) {
        try {
            response.onError(getEhp().transform(error));
        } finally {
            streamCollector.remove(response);
        }
    }

    private static <I> StreamObserver<I> getStreamObserverFeedingProcessor(UnicastProcessor<I> input) {
        StreamObserver<I> result = new StreamObserver<>() {
            @Override
            public void onNext(I i) {
                input.onNext(i);
            }

            @Override
            public void onError(Throwable throwable) {
                input.onError(throwable);
                streamCollector.remove(this);
            }

            @Override
            public void onCompleted() {
                input.onComplete();
                streamCollector.remove(this);
            }
        };

        streamCollector.add(result);

        return result;
    }

    private static void trySetCompression(StreamObserver<?> response, String compression) {
        if (compression != null && response instanceof ServerCallStreamObserver<?>) {
            ServerCallStreamObserver<?> serverResponse = (ServerCallStreamObserver<?>) response;
            serverResponse.setCompression(compression);
        }
    }

    // for dev mode only!

    public static void setStreamCollector(StreamCollector collector) {
        streamCollector = collector;
    }

    public static StreamCollector getStreamCollector() {
        return streamCollector;
    }
}
