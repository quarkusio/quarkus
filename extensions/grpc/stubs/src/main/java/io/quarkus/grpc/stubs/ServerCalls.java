package io.quarkus.grpc.stubs;

import java.util.function.Function;

import org.jboss.logging.Logger;

import io.grpc.Status;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import io.quarkus.arc.Arc;
import io.quarkus.grpc.ExceptionHandlerProvider;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.operators.multi.processors.UnicastProcessor;
import io.smallrye.mutiny.subscription.Cancellable;

public class ServerCalls {
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
            handleSubscription(uni.subscribe().with(
                    item -> {
                        response.onNext(item);
                        onCompleted(response);
                    },
                    failure -> onError(response, failure)), response);
        } catch (Throwable t) {
            onError(response, t);
        }
    }

    public static <I, O> void oneToMany(I request, StreamObserver<O> response, String compression,
            Function<I, Multi<O>> implementation) {
        try {
            trySetCompression(response, compression);
            streamCollector.add(response);
            Multi<O> returnValue = implementation.apply(request);
            if (returnValue == null) {
                log.error("gRPC service method returned null instead of Multi. " +
                        "Please change the implementation to return a Multi object or throw StatusRuntimeException");
                onError(response, Status.fromCode(Status.Code.INTERNAL).asException());
                return;
            }
            handleSubscription(returnValue.subscribe().with(
                    response::onNext,
                    throwable -> onError(response, throwable),
                    () -> onCompleted(response)), response);
        } catch (Throwable throwable) {
            onError(response, throwable);
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
            handleSubscription(uni.subscribe().with(
                    item -> {
                        response.onNext(item);
                        onCompleted(response);
                    },
                    failure -> onError(response, failure)), response);
            return pump;
        } catch (Throwable throwable) {
            onError(response, throwable);
            return null;
        }
    }

    private static <O> void handleSubscription(Cancellable cancellable, StreamObserver<O> response) {
        if (response instanceof ServerCallStreamObserver) {
            ServerCallStreamObserver<O> serverCallResponse = (ServerCallStreamObserver<O>) response;

            Runnable cancel = cancellable::cancel;

            serverCallResponse.setOnCloseHandler(cancel);
            serverCallResponse.setOnCancelHandler(cancel);
        }
    }

    public static <I, O> StreamObserver<I> manyToMany(StreamObserver<O> response,
            Function<Multi<I>, Multi<O>> implementation) {
        try {
            streamCollector.add(response);
            UnicastProcessor<I> input = UnicastProcessor.create();
            StreamObserver<I> pump = getStreamObserverFeedingProcessor(input);
            Multi<O> multi = implementation.apply(input);
            if (multi == null) {
                log.error("gRPC service method returned null instead of Multi. " +
                        "Please change the implementation to return a Multi object or throw StatusRuntimeException");
                onError(response, Status.fromCode(Status.Code.INTERNAL).asException());
                return null;
            }
            handleSubscription(multi.subscribe().with(
                    response::onNext,
                    failure -> onError(response, failure),
                    () -> onCompleted(response)), response);

            return pump;
        } catch (Throwable throwable) {
            onError(response, throwable);
            return null;
        }
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
