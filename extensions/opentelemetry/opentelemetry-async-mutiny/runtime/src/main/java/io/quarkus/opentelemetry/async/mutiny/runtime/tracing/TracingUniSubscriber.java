package io.quarkus.opentelemetry.async.mutiny.runtime.tracing;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;

public class TracingUniSubscriber<T> implements UniSubscriber<T> {

    private final Context context;

    private final UniSubscriber<? super T> subscriber;

    public TracingUniSubscriber(final Context context, final UniSubscriber<? super T> subscriber) {
        this.context = context;
        this.subscriber = subscriber;
    }

    @Override
    public void onSubscribe(final UniSubscription subscription) {
        subscriber.onSubscribe(subscription);
    }

    @Override
    public void onItem(final T item) {
        try (final Scope ignored = context.makeCurrent()) {
            subscriber.onItem(item);
        }
    }

    @Override
    public void onFailure(final Throwable failure) {
        try (final Scope ignored = context.makeCurrent()) {
            subscriber.onFailure(failure);
        }
    }
}
