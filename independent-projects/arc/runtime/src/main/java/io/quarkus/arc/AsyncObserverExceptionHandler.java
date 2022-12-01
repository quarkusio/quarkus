package io.quarkus.arc;

import javax.enterprise.inject.spi.EventContext;
import javax.enterprise.inject.spi.ObserverMethod;

/**
 * Handles an exception thrown by an asynchronous observer. By default, an error message is logged.
 * <p>
 * Note that the exception aborts processing of the observer but not of the async event.
 * <p>
 * A bean that implements this interface should be {@link javax.inject.Singleton} or
 * {@link javax.enterprise.context.ApplicationScoped}.
 *
 * @see javax.enterprise.event.ObservesAsync
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
