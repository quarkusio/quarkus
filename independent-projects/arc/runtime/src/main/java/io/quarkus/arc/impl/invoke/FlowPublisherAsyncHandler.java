package io.quarkus.arc.impl.invoke;

import java.util.concurrent.Flow;

import jakarta.enterprise.invoke.AsyncHandler;

public class FlowPublisherAsyncHandler<T> implements AsyncHandler.ReturnType<Flow.Publisher<T>> {
    @Override
    public Flow.Publisher<T> transform(Flow.Publisher<T> originalPublisher, Runnable completion) {
        return new Flow.Publisher<T>() {
            @Override
            public void subscribe(Flow.Subscriber<? super T> originalSubscriber) {
                originalPublisher.subscribe(new Flow.Subscriber<T>() {
                    @Override
                    public void onSubscribe(Flow.Subscription originalSubscription) {
                        originalSubscriber.onSubscribe(new Flow.Subscription() {
                            @Override
                            public void request(long n) {
                                originalSubscription.request(n);
                            }

                            @Override
                            public void cancel() {
                                completion.run();
                                originalSubscription.cancel();
                            }
                        });
                    }

                    @Override
                    public void onNext(T item) {
                        originalSubscriber.onNext(item);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        completion.run();
                        originalSubscriber.onError(throwable);
                    }

                    @Override
                    public void onComplete() {
                        completion.run();
                        originalSubscriber.onComplete();
                    }
                });
            }
        };
    }
}
