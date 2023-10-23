package io.quarkus.opentelemetry.runtime.tracing.intrumentation.reactivemessaging;

import static io.quarkus.opentelemetry.runtime.tracing.intrumentation.reactivemessaging.ReactiveMessagingTracingOutgoingDecorator.decorateOutgoing;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Message;

import io.smallrye.mutiny.Multi;
import io.smallrye.reactive.messaging.ChannelRegistry;
import io.smallrye.reactive.messaging.PublisherDecorator;

/**
 * Intercepts outgoing messages from emitters from Reactive Messaging connectors.
 * <p>
 * For outgoing messages from emitters, if the message doesn't already contain a tracing metadata, it attaches one with the
 * current
 * OpenTelemetry context.
 * Reactive messaging outbound connectors, if tracing is supported, will use that context as parent span to trace outbound
 * message transmission.
 */
@ApplicationScoped
public class ReactiveMessagingTracingEmitterDecorator implements PublisherDecorator {

    @Override
    public int getPriority() {
        // Place the decorator before all others including the ContextDecorator which is priority 0
        // This is only important for the emitter case
        return -1000;
    }

    @Inject
    ChannelRegistry registry;

    /**
     * Incoming messages
     */
    @Override
    public Multi<? extends Message<?>> decorate(Multi<? extends Message<?>> publisher,
            String channelName, boolean isConnector) {
        Multi<? extends Message<?>> multi = publisher;
        if (!isConnector && registry.getEmitterNames().contains(channelName)) {
            // Emitter is a special case for the emitter publisher
            multi = decorateOutgoing(multi);
        }
        return multi;
    }

}
