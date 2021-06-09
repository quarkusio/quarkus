package io.quarkus.grpc.runtime;

import java.util.function.Consumer;
import java.util.function.Function;

import org.jboss.logging.Logger;

import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.operators.multi.processors.UnicastProcessor;

public class ServerCalls {
    private static final Logger log = Logger.getLogger(ServerCalls.class);

    private static StreamCollector streamCollector = StreamCollector.NO_OP;

    private ServerCalls() {
    }

    public static <I, O> void oneToOne(I request, StreamObserver<O> response, String compression,
            Function<I, Uni<O>> implementation) {
        trySetCompression(response, compression);
        try {
            Uni<O> uni = implementation.apply(request);
            if (uni == null) {
                log.error("gRPC service method returned null instead of Uni. " +
                        "Please change the implementation to return a Uni object, either carrying a value or a failure," +
                        " or throw StatusRuntimeException");
                response.onError(Status.fromCode(Status.Code.INTERNAL).asException());
                return;
            }
            uni.subscribe().with(
                    new Consumer<O>() {
                        @Override
                        public void accept(O item) {
                            response.onNext(item);
                            response.onCompleted();
                        }
                    },
                    new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable failure) {
                            response.onError(toStatusFailure(failure));
                        }
                    });
        } catch (Throwable throwable) {
            response.onError(toStatusFailure(throwable));
        }
    }

    public static <I, O> void oneToMany(I request, StreamObserver<O> response, String compression,
            Function<I, Multi<O>> implementation) {
        try {
            streamCollector.add(response);
            Multi<O> returnValue = implementation.apply(request);
            if (returnValue == null) {
                log.error("gRPC service method returned null instead of Multi. " +
                        "Please change the implementation to return a Multi object or throw StatusRuntimeException");
                response.onError(Status.fromCode(Status.Code.INTERNAL).asException());
                return;
            }
            returnValue.subscribe().with(
                    new Consumer<O>() {
                        @Override
                        public void accept(O v) {
                            response.onNext(v);
                        }
                    },
                    new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable throwable) {
                            onError(response, throwable);
                        }
                    },
                    new Runnable() {
                        @Override
                        public void run() {
                            onCompleted(response);
                        }
                    });
        } catch (Throwable throwable) {
            onError(response, toStatusFailure(throwable));
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
                response.onError(Status.fromCode(Status.Code.INTERNAL).asException());
                return null;
            }
            uni.subscribe().with(
                    new Consumer<O>() {
                        @Override
                        public void accept(O item) {
                            response.onNext(item);
                            onCompleted(response);
                        }
                    },
                    new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable failure) {
                            onError(response, toStatusFailure(failure));
                        }
                    });
            return pump;
        } catch (Throwable throwable) {
            response.onError(toStatusFailure(throwable));
            return null;
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
                response.onError(Status.fromCode(Status.Code.INTERNAL).asException());
                return null;
            }
            multi.subscribe().with(
                    new Consumer<O>() {
                        @Override
                        public void accept(O v) {
                            response.onNext(v);
                        }
                    },
                    new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable failure) {
                            onError(response, toStatusFailure(failure));
                        }
                    },
                    new Runnable() {
                        @Override
                        public void run() {
                            onCompleted(response);
                        }
                    });
            return pump;
        } catch (Throwable throwable) {
            onError(response, toStatusFailure(throwable));
            return null;
        }
    }

    private static <O> void onCompleted(StreamObserver<O> response) {
        response.onCompleted();
        streamCollector.remove(response);
    }

    private static <O> void onError(StreamObserver<O> response, Throwable error) {
        response.onError(error);
        streamCollector.remove(response);
    }

    private static <I> StreamObserver<I> getStreamObserverFeedingProcessor(UnicastProcessor<I> input) {
        StreamObserver<I> result = new StreamObserver<I>() {
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

    private static Throwable toStatusFailure(Throwable throwable) {
        if (throwable instanceof StatusException || throwable instanceof StatusRuntimeException) {
            return throwable;
        } else {
            String desc = throwable.getClass().getName();
            if (throwable.getMessage() != null) {
                desc += " - " + throwable.getMessage();
            }
            if (throwable instanceof IllegalArgumentException) {
                return Status.INVALID_ARGUMENT.withDescription(desc).asException();
            }
            log.warn("gRPC service threw an exception other than StatusRuntimeException", throwable);
            return Status.fromThrowable(throwable)
                    .withDescription(desc)
                    .asException();
        }
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
