package io.quarkus.opentelemetry.runtime.tracing.intrumentation.reactivemessaging;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Message;

import io.quarkus.opentelemetry.runtime.QuarkusContextStorage;
import io.smallrye.mutiny.Multi;
import io.smallrye.reactive.messaging.SubscriberDecorator;
import io.smallrye.reactive.messaging.TracingMetadata;

/**
 * Intercepts outgoing messages from Reactive Messaging connectors.
 * <p>
 * For outgoing messages, if the message doesn't already contain a tracing metadata, it attaches one with the current
 * OpenTelemetry context.
 * Reactive messaging outbound connectors, if tracing is supported, will use that context as parent span to trace outbound
 * message transmission.
 */
@ApplicationScoped
public class ReactiveMessagingTracingOutgoingDecorator implements SubscriberDecorator {

    /**
     * Outgoing messages
     */
    @Override
    public Multi<? extends Message<?>> decorate(Multi<? extends Message<?>> toBeSubscribed,
            List<String> channelName, boolean isConnector) {
        Multi<? extends Message<?>> multi = toBeSubscribed;
        if (isConnector) {
            // add TracingMetadata to the outgoing message if it doesn't exist already
            multi = multi.map(m -> {
                Message<?> message = m;
                if (m.getMetadata(TracingMetadata.class).isEmpty()) {
                    var otelContext = QuarkusContextStorage.INSTANCE.current();
                    message = m.addMetadata(TracingMetadata.withCurrent(otelContext));
                }
                return message;
            });
        }
        return multi;
    }

}
