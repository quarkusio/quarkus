package io.quarkus.vertx.http.runtime;

import io.vertx.core.Handler;

/**
 * The type of route handler
 */
public enum HandlerType {

    /**
     * A regular route handler invoked on the event loop.
     *
     * @see io.vertx.ext.web.Route#handler(Handler)
     */
    NORMAL,
    /**
     * A blocking route handler, invoked on a worker thread.
     *
     * @see io.vertx.ext.web.Route#blockingHandler(Handler)
     */
    BLOCKING,
    /**
     * A failure handler, invoked when an exception is thrown from a route handler.
     * This is invoked on the event loop.
     *
     * @see io.vertx.ext.web.Route#failureHandler(Handler)
     */
    FAILURE

}
