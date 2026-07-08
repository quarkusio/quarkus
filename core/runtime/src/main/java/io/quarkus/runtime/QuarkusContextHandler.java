package io.quarkus.runtime;

import org.jboss.threads.ContextHandler;

/**
 * A non-generic sub-interface of {@link ContextHandler} to support
 * ActionBuilder service registration without generic types.
 */
public interface QuarkusContextHandler extends ContextHandler<Object> {

    /**
     * Create a QuarkusContextHandler that delegates to a raw JBoss Threads ContextHandler.
     *
     * @param delegate the delegate context handler (must not be {@code null})
     * @return the delegating QuarkusContextHandler
     */
    static QuarkusContextHandler of(ContextHandler<Object> delegate) {
        return new QuarkusContextHandler() {
            @Override
            public Object captureContext() {
                return delegate.captureContext();
            }

            @Override
            public void runWith(Runnable task, Object state) {
                delegate.runWith(task, state);
            }
        };
    }
}
