package io.quarkus.opentelemetry.async.mutiny.runtime.tracing;

import static io.opentelemetry.instrumentation.api.annotation.support.async.AsyncOperationEndSupport.tryToGetResponse;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.annotation.support.async.AsyncOperationEndStrategy;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.quarkus.opentelemetry.async.mutiny.runtime.MutinyAsyncConfig;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public class MutinyAsyncOperationEndStrategy implements AsyncOperationEndStrategy {

    private final MutinyAsyncConfig.MutinyAsyncRuntimeConfig config;

    public MutinyAsyncOperationEndStrategy(final MutinyAsyncConfig.MutinyAsyncRuntimeConfig config) {
        this.config = config;
    }

    @Override
    public boolean supports(final Class<?> asyncType) {
        return asyncType == Uni.class || asyncType == Multi.class;
    }

    @Override
    public <REQUEST, RESPONSE> Object end(final Instrumenter<REQUEST, RESPONSE> instrumenter, final Context context,
            final REQUEST request, final Object asyncValue, final Class<RESPONSE> responseType) {

        final EndOnFirstNotificationConsumer<Object> consumer = new EndOnFirstNotificationConsumer<>(context, config) {
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
                .onSubscription().invoke(() -> addSpanAttributeOnSubscription(consumer.getContext()))
                .onTermination().invoke((EndOnFirstNotificationConsumer<T>) consumer);
    }

    private Multi<?> endWhenMulti(final Multi<?> asyncValue, final EndOnFirstNotificationConsumer<Object> consumer) {
        return asyncValue
                .onSubscription().invoke(() -> addSpanAttributeOnSubscription(consumer.getContext()))
                .onTermination().invoke(consumer);
    }

    private void addSpanAttributeOnSubscription(final Context context) {
        this.config.event.onSubscribe.ifPresent(value -> Span.fromContext(context).addEvent(value));
    }
}
