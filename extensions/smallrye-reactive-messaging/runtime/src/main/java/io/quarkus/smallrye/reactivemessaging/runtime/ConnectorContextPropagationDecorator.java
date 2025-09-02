package io.quarkus.smallrye.reactivemessaging.runtime;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.context.ThreadContext;
import org.eclipse.microprofile.reactive.messaging.Message;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.operators.multi.AbstractMultiOperator;
import io.smallrye.mutiny.operators.multi.MultiOperatorProcessor;
import io.smallrye.mutiny.subscription.MultiSubscriber;
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
            return new ContextPropagationOperator<>(publisher, tc);
        }
        return publisher;
    }

    @Override
    public int getPriority() {
        // Before the io.smallrye.reactive.messaging.providers.locals.ContextDecorator which has the priority 0
        return -100;
    }

    public static class ContextPropagationOperator<T> extends AbstractMultiOperator<T, T> {

        private final ThreadContext tc;

        /**
         * Creates a new {@link AbstractMultiOperator} with the passed {@link Multi} as upstream.
         *
         * @param upstream the upstream, must not be {@code null}
         */
        public ContextPropagationOperator(Multi<? extends T> upstream, ThreadContext tc) {
            super(upstream);
            this.tc = tc;
        }

        @Override
        public void subscribe(MultiSubscriber<? super T> downstream) {
            ParameterValidation.nonNullNpe(downstream, "subscriber");
            upstream.subscribe().withSubscriber(new ContextPropagationProcessor<>(downstream, tc));
        }

        static final class ContextPropagationProcessor<T> extends MultiOperatorProcessor<T, T> {

            private final Executor tcExecutor;

            public ContextPropagationProcessor(MultiSubscriber<? super T> downstream, ThreadContext tc) {
                super(downstream);
                this.tcExecutor = tc.currentContextExecutor();
            }

            @Override
            public void onItem(T item) {
                // Even though the executor is called, this is a synchronous call
                tcExecutor.execute(() -> super.onItem(item));
            }

        }
    }
}
