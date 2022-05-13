package io.quarkus.opentelemetry.async.mutiny.runtime;

import static io.opentelemetry.instrumentation.api.annotation.support.async.AsyncOperationEndSupport.tryToGetResponse;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.annotation.support.async.AsyncOperationEndStrategy;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public class MutinyAsyncOperationEndStrategy implements AsyncOperationEndStrategy {

    @Override
    public boolean supports(final Class<?> asyncType) {
        return asyncType == Uni.class || asyncType == Multi.class;
    }

    @Override
    public <REQUEST, RESPONSE> Object end(final Instrumenter<REQUEST, RESPONSE> instrumenter, final Context context,
            final REQUEST request, final Object asyncValue, final Class<RESPONSE> responseType) {

        final EndOnFirstNotificationConsumer<Object> consumer = new EndOnFirstNotificationConsumer<>(context) {
            @Override
            protected void end(final Object response, final Throwable error) {
                instrumenter.end(context, request, tryToGetResponse(responseType, response), error);
            }
        };

        // TODO: Subscribed event as config
        if (asyncValue instanceof Uni) {
            return endWhenUni((Uni<?>) asyncValue, consumer);
        } else {
            return endWhenMulti((Multi<?>) asyncValue, consumer);
        }
    }

    // We know that the consumer will consume the item type of the uni
    @SuppressWarnings("unchecked")
    private <T> Uni<T> endWhenUni(final Uni<T> asyncValue, final EndOnFirstNotificationConsumer<?> consumer) {
        return asyncValue
                .onSubscription().invoke(() -> Span.fromContext(consumer.getContext()).addEvent("Subscribed"))
                .onTermination().invoke((EndOnFirstNotificationConsumer<T>) consumer);
    }

    private Multi<?> endWhenMulti(final Multi<?> asyncValue, final EndOnFirstNotificationConsumer<Object> consumer) {
        return asyncValue
                .onSubscription().invoke(() -> Span.fromContext(consumer.getContext()).addEvent("Subscribed"))
                .onTermination().invoke(consumer);
    }
}
