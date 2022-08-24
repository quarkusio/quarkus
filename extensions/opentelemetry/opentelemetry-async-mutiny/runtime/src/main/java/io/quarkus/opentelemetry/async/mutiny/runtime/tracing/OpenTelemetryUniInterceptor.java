package io.quarkus.opentelemetry.async.mutiny.runtime.tracing;

import io.opentelemetry.context.Context;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.UniInterceptor;
import io.smallrye.mutiny.subscription.UniSubscriber;

public class OpenTelemetryUniInterceptor implements UniInterceptor {

    @Override
    public <T> Uni<T> onUniCreation(final Uni<T> uni) {
        if (uni instanceof TracingUni) {
            return uni;
        }
        return new TracingUni<>(Context.current(), uni);
    }

    @Override
    public <T> UniSubscriber<? super T> onSubscription(final Uni<T> instance,
            final UniSubscriber<? super T> subscriber) {
        if (subscriber instanceof TracingUniSubscriber) {
            return subscriber;
        }
        return new TracingUniSubscriber<>(Context.current(), subscriber);
    }
}
