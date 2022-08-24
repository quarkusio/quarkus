package io.quarkus.opentelemetry.async.mutiny.runtime.tracing;

import org.reactivestreams.Subscriber;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.AbstractMulti;

public class TracingMulti<T> extends AbstractMulti<T> {

    private final Context context;

    private final Multi<T> multi;

    public TracingMulti(final Context context, final Multi<T> multi) {
        this.context = context;
        this.multi = multi;
    }

    @Override
    public void subscribe(final Subscriber<? super T> subscriber) {
        try (final Scope ignored = context.makeCurrent()) {
            multi.subscribe(new TracingMultiSubscriber<>(context, subscriber));
        }
    }
}
