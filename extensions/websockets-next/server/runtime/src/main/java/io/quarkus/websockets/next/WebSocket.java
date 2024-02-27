package io.quarkus.websockets.next;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Denotes a WebSocket endpoint.
 */
@Retention(RUNTIME)
@Target(TYPE)
public @interface WebSocket {

    /**
     * @return the path
     */
    public String path();

    /**
     * @return the execution mode
     */
    public ExecutionMode executionMode() default ExecutionMode.SERIAL;

    /**
     * Execution mode used during message processing.
     */
    enum ExecutionMode {

        /**
         * Messages are processed serially, ordering is guaranteed.
         */
        SERIAL,

        /**
         * Messages are processed concurrently, there are no ordering guarantees.
         */
        CONCURRENT,

    }

}
