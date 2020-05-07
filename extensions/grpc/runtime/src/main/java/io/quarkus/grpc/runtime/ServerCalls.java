package io.quarkus.grpc.runtime;

import java.util.function.Consumer;
import java.util.function.Function;

import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.operators.multi.processors.UnicastProcessor;

public class ServerCalls {

    private ServerCalls() {
    }

    public static <I, O> void oneToOne(I request, StreamObserver<O> response, Function<I, Uni<O>> implementation) {
        try {
            Uni<O> uni = implementation.apply(request);
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

    public static <I, O> void oneToMany(I request, StreamObserver<O> response, Function<I, Multi<O>> implementation) {
        try {
            implementation.apply(request)
                    .subscribe().with(
                            new Consumer<O>() {
                                @Override
                                public void accept(O v) {
                                    response.onNext(v);
                                }
                            },
                            new Consumer<Throwable>() {
                                @Override
                                public void accept(Throwable throwable) {
                                    response.onError(throwable);
                                }
                            },
                            new Runnable() {
                                @Override
                                public void run() {
                                    response.onCompleted();
                                }
                            });
        } catch (Throwable throwable) {
            response.onError(toStatusFailure(throwable));
        }
    }

    public static <I, O> StreamObserver<I> manyToOne(StreamObserver<O> response,
            Function<Multi<I>, Uni<O>> implementation) {
        try {
            UnicastProcessor<I> input = UnicastProcessor.create();
            StreamObserver<I> pump = getStreamObserverFeedingProcessor(input);
            Uni<O> uni = implementation.apply(input);
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
            return pump;
        } catch (Throwable throwable) {
            response.onError(toStatusFailure(throwable));
            return null;
        }
    }

    public static <I, O> StreamObserver<I> manyToMany(StreamObserver<O> response,
            Function<Multi<I>, Multi<O>> implementation) {
        try {
            UnicastProcessor<I> input = UnicastProcessor.create();
            StreamObserver<I> pump = getStreamObserverFeedingProcessor(input);
            Multi<O> uni = implementation.apply(input);
            uni.subscribe().with(
                    new Consumer<O>() {
                        @Override
                        public void accept(O v) {
                            response.onNext(v);
                        }
                    },
                    new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable failure) {
                            response.onError(toStatusFailure(failure));
                        }
                    },
                    new Runnable() {
                        @Override
                        public void run() {
                            response.onCompleted();
                        }
                    });
            return pump;
        } catch (Throwable throwable) {
            response.onError(toStatusFailure(throwable));
            return null;
        }
    }

    private static <I> StreamObserver<I> getStreamObserverFeedingProcessor(UnicastProcessor<I> input) {
        return new StreamObserver<I>() {
            @Override
            public void onNext(I i) {
                input.onNext(i);
            }

            @Override
            public void onError(Throwable throwable) {
                input.onError(throwable);
            }

            @Override
            public void onCompleted() {
                input.onComplete();
            }
        };
    }

    private static Throwable toStatusFailure(Throwable throwable) {
        if (throwable instanceof StatusException || throwable instanceof StatusRuntimeException) {
            return throwable;
        } else {
            return Status.fromThrowable(throwable).asException();
        }
    }
}
