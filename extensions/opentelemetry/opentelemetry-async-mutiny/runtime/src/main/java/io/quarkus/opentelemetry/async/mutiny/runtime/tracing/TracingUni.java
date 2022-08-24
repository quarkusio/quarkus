package io.quarkus.opentelemetry.async.mutiny.runtime.tracing;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.operators.AbstractUni;
import io.smallrye.mutiny.subscription.UniSubscriber;

public class TracingUni<T> extends AbstractUni<T> {

    private final Context context;

    private final Uni<T> uni;

    public TracingUni(final Context context, final Uni<T> uni) {
        this.context = context;
        this.uni = uni;
    }

    @Override
    public void subscribe(final UniSubscriber<? super T> subscriber) {
        try (final Scope ignored = context.makeCurrent()) {
            uni.subscribe().withSubscriber(new TracingUniSubscriber<>(context, subscriber));
        }
    }
}
