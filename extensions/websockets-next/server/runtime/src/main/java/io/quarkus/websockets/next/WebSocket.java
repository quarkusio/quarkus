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
     * The path of the endpoint.
     * <p>
     * It is possible to match path parameters. The placeholder of a path parameter consists of the parameter name surrounded by
     * curly brackets. The actual value of a path parameter can be obtained using
     * {@link WebSocketServerConnection#pathParam(String)}. For example, the path <code>/foo/{bar}</code> defines the path
     * parameter {@code bar}.
     *
     * @see WebSocketServerConnection#pathParam(String)
     */
    public String path();

    /**
     * The execution mode used to process incoming messages for a specific connection.
     */
    public ExecutionMode executionMode() default ExecutionMode.SERIAL;

    /**
     * Defines the execution mode used to process incoming messages for a specific connection.
     *
     * @see WebSocketServerConnection
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
