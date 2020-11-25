package org.jboss.resteasy.reactive.server.core.parameters;

import java.util.function.BiConsumer;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;

public interface ParameterExtractor {

    /**
     * Extracts a parameter from the request.
     *
     * If this returns a {@link ParameterCallback} then the value must be obtained from the listener
     *
     */
    Object extractParameter(ResteasyReactiveRequestContext context);

    /**
     * listener class that is used to provide async method parameters.
     * 
     * This is very simple to reduce the number of required allocations.
     */
    public class ParameterCallback {

        private Object result;
        private Exception failure;
        private BiConsumer<Object, Exception> listener;

        public void setListener(BiConsumer<Object, Exception> listener) {
            Object result;
            Exception failure;
            synchronized (this) {
                if (this.listener != null) {
                    throw new RuntimeException("Listener already set");
                }
                result = this.result;
                failure = this.failure;
                this.listener = listener;
            }
            if (result != null || failure != null) {
                listener.accept(result, failure);
            }
        }

        public void setResult(Object result) {
            BiConsumer<Object, Exception> listener;
            synchronized (this) {
                if (this.result != null || this.failure != null) {
                    throw new RuntimeException("Callback already completed result:" + result + " failure:" + failure);
                }
                listener = this.listener;
                this.result = result;
            }
            if (listener != null) {
                listener.accept(result, null);
            }
        }

        public void setFailure(Exception failure) {
            BiConsumer<Object, Exception> listener;
            synchronized (this) {
                if (this.result != null || this.failure != null) {
                    throw new RuntimeException("Callback already completed result:" + result + " failure:" + failure);
                }
                listener = this.listener;
                this.failure = failure;
            }
            if (listener != null) {
                listener.accept(null, failure);
            }
        }

    }

}
