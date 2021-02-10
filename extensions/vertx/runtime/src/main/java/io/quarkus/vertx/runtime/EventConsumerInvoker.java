package io.quarkus.vertx.runtime;

import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableContext.ContextState;
import io.quarkus.arc.ManagedContext;
import io.quarkus.vertx.ConsumeEvent;
import io.vertx.core.eventbus.Message;

/**
 * Invokes a business method annotated with {@link ConsumeEvent}.
 */
public abstract class EventConsumerInvoker {

    public boolean isBlocking() {
        return false;
    }

    public void invoke(Message<Object> message) throws Exception {
        ManagedContext requestContext = Arc.container().requestContext();
        if (requestContext.isActive()) {
            CompletionStage<Object> ret = invokeBean(message);
            if (ret != null) {
                ret.whenComplete(new RequestActiveConsumer(message));
            }
        } else {
            // Activate the request context
            requestContext.activate();
            CompletionStage<Object> ret;
            try {
                ret = invokeBean(message);
            } catch (Exception e) {
                // Terminate the request context and re-throw the exception
                requestContext.terminate();
                throw e;
            }
            if (ret == null) {
                // No async computation - just terminate
                requestContext.terminate();
            } else {
                // Capture the state, deactivate and destroy the context when the computation completes
                ContextState endState = requestContext.getState();
                requestContext.deactivate();
                ret.whenComplete(new RequestActivatedConsumer(message, requestContext, endState));
            }
        }
    }

    protected abstract CompletionStage<Object> invokeBean(Message<Object> message) throws Exception;

    private static class RequestActiveConsumer implements BiConsumer<Object, Throwable> {

        private final Message<Object> message;

        RequestActiveConsumer(Message<Object> message) {
            this.message = message;
        }

        @Override
        public void accept(Object result, Throwable failure) {
            if (failure != null) {
                if (message.replyAddress() == null) {
                    // No reply handler
                    throw VertxRecorder.wrapIfNecessary(failure);
                } else {
                    message.fail(ConsumeEvent.EXPLICIT_FAILURE_CODE, failure.getMessage());
                }
            } else {
                message.reply(result);
            }
        }

    }

    private static class RequestActivatedConsumer implements BiConsumer<Object, Throwable> {

        private final Message<Object> message;
        private final ManagedContext requestContext;
        private final ContextState endState;

        public RequestActivatedConsumer(Message<Object> message, ManagedContext requestContext, ContextState endState) {
            this.message = message;
            this.requestContext = requestContext;
            this.endState = endState;
        }

        @Override
        public void accept(Object result, Throwable failure) {
            try {
                requestContext.destroy(endState);
            } catch (Exception e) {
                throw VertxRecorder.wrapIfNecessary(e);
            }
            if (failure != null) {
                if (message.replyAddress() == null) {
                    // No reply handler
                    throw VertxRecorder.wrapIfNecessary(failure);
                } else {
                    message.fail(ConsumeEvent.EXPLICIT_FAILURE_CODE, failure.getMessage());
                }
            } else {
                message.reply(result);
            }
        }

    }

}
