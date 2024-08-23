package io.quarkus.opentelemetry.runtime.tracing.intrumentation.reactivemessaging;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Message;

import io.opentelemetry.context.Context;
import io.quarkus.opentelemetry.runtime.QuarkusContextStorage;
import io.smallrye.mutiny.Multi;
import io.smallrye.reactive.messaging.PublisherDecorator;
import io.smallrye.reactive.messaging.TracingMetadata;
import io.smallrye.reactive.messaging.providers.locals.LocalContextMetadata;

/**
 * Intercepts incoming messages from Reactive Messaging connectors.
 * <p>
 * For incoming messages, it fetches OpenTelemetry context from the message and attaches to the duplicated context of the
 * message.
 * Consumer methods will be called on this duplicated context, so the OpenTelemetry context associated with the incoming message
 * will be propagated.
 */
@ApplicationScoped
public class ReactiveMessagingTracingIncomingDecorator implements PublisherDecorator {

    /**
     * Incoming messages
     */
    @Override
    public Multi<? extends Message<?>> decorate(Multi<? extends Message<?>> publisher,
            String channelName, boolean isConnector) {
        Multi<? extends Message<?>> multi = publisher;
        if (isConnector) {
            // attach OTel context from incoming message to the duplicated message context
            multi = multi.invoke(m -> {
                var messageContext = m.getMetadata(LocalContextMetadata.class)
                        .map(LocalContextMetadata::context)
                        .orElse(null);
                var otelContext = TracingMetadata.fromMessage(m)
                        .map(TracingMetadata::getCurrentContext)
                        .orElse(Context.current());
                if (messageContext != null && otelContext != null) {
                    QuarkusContextStorage.INSTANCE.attach(messageContext, otelContext);
                }
            });
        }
        return multi;
    }

}
