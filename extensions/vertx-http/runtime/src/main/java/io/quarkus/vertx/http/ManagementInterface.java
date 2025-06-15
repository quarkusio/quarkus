package io.quarkus.vertx.http;

import io.vertx.ext.web.Router;

/**
 * A class allowing to access the management router. You can access the instance of this class using a CDI observer:
 * {@code public void init(@Observe ManagementInterface mi) {...}}
 * <p>
 * If the management interface is disabled, the event is not fired.
 */
public interface ManagementInterface {

    /**
     * @return the management router
     */
    Router router();
}
