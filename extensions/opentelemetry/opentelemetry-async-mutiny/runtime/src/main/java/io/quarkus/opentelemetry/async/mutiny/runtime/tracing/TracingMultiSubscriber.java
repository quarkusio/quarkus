package io.quarkus.opentelemetry.async.mutiny.runtime.tracing;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.smallrye.mutiny.subscription.MultiSubscriber;

public class TracingMultiSubscriber<T> implements MultiSubscriber<T> {

    private final Context context;

    private final Subscriber<? super T> subscriber;

    public TracingMultiSubscriber(final Context context, final Subscriber<? super T> subscriber) {
        this.context = context;
        this.subscriber = subscriber;
    }

    @Override
    public void onSubscribe(final Subscription subscription) {
        subscriber.onSubscribe(subscription);
    }

    @Override
    public void onItem(final T item) {
        try (final Scope ignored = context.makeCurrent()) {
            subscriber.onNext(item);
        }
    }

    @Override
    public void onFailure(final Throwable failure) {
        try (final Scope ignored = context.makeCurrent()) {
            subscriber.onError(failure);
        }
    }

    @Override
    public void onCompletion() {
        try (final Scope ignored = context.makeCurrent()) {
            subscriber.onComplete();
        }
    }
}
