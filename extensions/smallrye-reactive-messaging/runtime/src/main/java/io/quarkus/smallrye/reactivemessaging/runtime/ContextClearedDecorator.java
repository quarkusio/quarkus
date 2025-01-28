package io.quarkus.smallrye.reactivemessaging.runtime;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.context.ThreadContext;
import org.eclipse.microprofile.reactive.messaging.Message;

import io.smallrye.mutiny.Multi;
import io.smallrye.reactive.messaging.PublisherDecorator;

@ApplicationScoped
public class ContextClearedDecorator implements PublisherDecorator {

    private final ThreadContext tc;

    public ContextClearedDecorator() {
        tc = ThreadContext.builder()
                .propagated(ThreadContext.NONE)
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
