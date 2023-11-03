package io.quarkus.arc;

import jakarta.enterprise.inject.spi.EventContext;
import jakarta.enterprise.inject.spi.ObserverMethod;

/**
 * Handles an exception thrown by an asynchronous observer. By default, an error message is logged.
 * <p>
 * Note that the exception aborts processing of the observer but not of the async event.
 * <p>
 * A bean that implements this interface should be {@link jakarta.inject.Singleton} or
 * {@link jakarta.enterprise.context.ApplicationScoped}.
 *
 * @see jakarta.enterprise.event.ObservesAsync
 */
public interface AsyncObserverExceptionHandler {

    /**
     *
     * @param throwable
     * @param observerMethod
     * @param eventContext
     */
    void handle(Throwable throwable, ObserverMethod<?> observerMethod, EventContext<?> eventContext);

}
