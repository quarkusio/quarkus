package io.quarkus.smallrye.reactivemessaging.runtime;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.context.ThreadContext;
import org.eclipse.microprofile.reactive.messaging.Message;

import io.smallrye.mutiny.Multi;
import io.smallrye.reactive.messaging.PublisherDecorator;
import io.smallrye.reactive.messaging.SubscriberDecorator;

@ApplicationScoped
public class ConnectorContextPropagationDecorator implements PublisherDecorator, SubscriberDecorator {

    private final ThreadContext tc;

    @Inject
    public ConnectorContextPropagationDecorator(
            @ConfigProperty(name = "quarkus.messaging.connector-context-propagation") Optional<List<String>> propagation) {
        tc = ThreadContext.builder()
                .propagated(propagation.map(l -> l.toArray(String[]::new)).orElse(ThreadContext.NONE))
                .cleared(ThreadContext.ALL_REMAINING)
                .build();
    }

    @Override
    public Multi<? extends Message<?>> decorate(Multi<? extends Message<?>> publisher, List<String> channelName,
            boolean isConnector) {
        if (isConnector) {
            return publisher.emitOn(tc.currentContextExecutor());
        }
        return publisher;
    }

    @Override
    public int getPriority() {
        // Before the io.smallrye.reactive.messaging.providers.locals.ContextDecorator which has the priority 0
        return -100;
    }
}
