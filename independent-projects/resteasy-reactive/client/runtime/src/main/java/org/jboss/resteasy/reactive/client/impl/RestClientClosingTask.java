package org.jboss.resteasy.reactive.client.impl;

import java.util.ServiceLoader;

import org.jboss.logging.Logger;

/**
 * This represents a task that will be called by the implementation class of the REST Client when the {@code close}
 * method is called.
 */
public interface RestClientClosingTask {

    Logger log = Logger.getLogger(RestClientClosingTask.class);

    void close(Context context);

    record Context(Class<?> restApiClass, WebTargetImpl baseTarget) {
    }

    @SuppressWarnings("unused") // this is called by the implementation class of the REST Client when the {@code close} method is called
    static void invokeAll(Context context) {
        for (RestClientClosingTask restClientClosingTask : ServiceLoader.load(RestClientClosingTask.class)) {
            try {
                restClientClosingTask.close(context);
            } catch (Exception e) {
                log.warn("Error running RestClientClosingTask", e);
            }
        }
    }
}
