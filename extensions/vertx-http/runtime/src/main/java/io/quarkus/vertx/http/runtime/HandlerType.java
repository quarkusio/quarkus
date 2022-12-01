package io.quarkus.vertx.http.runtime;

import io.vertx.core.Handler;

public enum HandlerType {

    /**
     * A request handler.
     *
     * @see io.vertx.ext.web.Route#handler(Handler)
     */
    NORMAL,
    /**
     * A blocking request handler.
     *
     * @see io.vertx.ext.web.Route#blockingHandler(Handler)
     */
    BLOCKING,
    /**
     * A failure handler.
     *
     * @see io.vertx.ext.web.Route#failureHandler(Handler)
     */
    FAILURE

}
