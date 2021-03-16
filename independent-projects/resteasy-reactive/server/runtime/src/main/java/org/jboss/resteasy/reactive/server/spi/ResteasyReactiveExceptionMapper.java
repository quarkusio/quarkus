package org.jboss.resteasy.reactive.server.spi;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

public interface ResteasyReactiveExceptionMapper<E extends Throwable> extends ExceptionMapper<E> {

    /**
     * Convenience method that allows for easy access to the request context
     */
    Response toResponse(E exception, ServerRequestContext context);
}
