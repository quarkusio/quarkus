package io.quarkus.scheduler.common.runtime;

import java.util.concurrent.CompletionStage;

import io.quarkus.scheduler.ScheduledExecution;

/**
 * Invokes a scheduled business method of a bean.
 */
public interface ScheduledInvoker {

    /**
     *
     * @param execution
     * @return the result
     * @throws Exception
     */
    CompletionStage<Void> invoke(ScheduledExecution execution) throws Exception;

    /**
     * A blocking invoker is executed on the main executor for blocking tasks.
     * A non-blocking invoker is executed on the event loop.
     *
     * @return {@code true} if the scheduled method is blocking, {@code false} otherwise
     */
    default boolean isBlocking() {
        return true;
    }

}
