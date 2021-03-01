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

    public boolean isOrdered() {
        return false;
    }

    public void invoke(Message<Object> message) throws Exception {
        ManagedContext requestContext = Arc.container().requestContext();
        if (requestContext.isActive()) {
            Object ret = invokeBean(message);
            if (ret != null) {
                if (ret instanceof CompletionStage) {
                    ((CompletionStage<?>) ret).whenComplete(new RequestActiveConsumer(message));
                } else {
                    message.reply(ret);
                }
            }
        } else {
            // Activate the request context
            requestContext.activate();
            Object ret;
            try {
                ret = invokeBean(message);
            } catch (Exception e) {
                // Terminate the request context and re-throw the exception
                requestContext.terminate();
                throw e;
            }
            if (ret == null) {
                // No result - just terminate
                requestContext.terminate();
            } else {
                if (ret instanceof CompletionStage) {
                    // Capture the state, deactivate and destroy the context when the computation completes
                    ContextState endState = requestContext.getState();
                    requestContext.deactivate();
                    ((CompletionStage<?>) ret).whenComplete(new RequestActivatedConsumer(message, requestContext, endState));
                } else {
                    // No async computation - just terminate and set reply
                    requestContext.terminate();
                    message.reply(ret);
                }
            }
        }
    }

    protected abstract Object invokeBean(Message<Object> message) throws Exception;

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
