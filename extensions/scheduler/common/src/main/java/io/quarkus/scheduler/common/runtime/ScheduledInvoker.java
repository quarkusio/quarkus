package io.quarkus.scheduler.common.runtime;

import java.util.concurrent.CompletionStage;

import io.quarkus.scheduler.ScheduledExecution;

/**
 * Invokes a scheduled business method of a bean.
 */
public interface ScheduledInvoker {

    /**
     * @param execution
     * @return the result, never {@code null}
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

    /**
     * Indicates that the invoker used the virtual thread executor to execute the tasks.
     * Note that the method must use a synchronous signature.
     *
     * @return {@code true} if the scheduled method runs on a virtual thread.
     */
    default boolean isRunningOnVirtualThread() {
        return false;
    }

}
