package io.quarkus.qrs.runtime.spi;

import io.quarkus.qrs.runtime.core.RequestContext;

public interface EndpointFactory {

    /**
     * Creates an endpoint instance outside the scope of a request
     */
    EndpointInstance createInstance();

    /**
     * Creates an endpoint instance inside the scope of a request
     */
    EndpointInstance createInstance(RequestContext requestContext);

    interface EndpointInstance extends AutoCloseable {

        Object getInstance();

        void close();
    }

}
