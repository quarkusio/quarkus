package io.quarkus.opentelemetry.async.mutiny.runtime.tracing;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import io.opentelemetry.context.Context;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.infrastructure.MultiInterceptor;

public class OpenTelemetryMultiInterceptor implements MultiInterceptor {

    @Override
    public <T> Multi<T> onMultiCreation(final Multi<T> multi) {
        if (multi instanceof TracingMulti) {
            return multi;
        }
        return new TracingMulti<>(Context.current(), multi);
    }

    @Override
    public <T> Subscriber<? super T> onSubscription(final Publisher<? extends T> instance,
            final Subscriber<? super T> subscriber) {
        if (subscriber instanceof TracingMultiSubscriber) {
            return subscriber;
        }
        return new TracingMultiSubscriber<>(Context.current(), subscriber);
    }
}
