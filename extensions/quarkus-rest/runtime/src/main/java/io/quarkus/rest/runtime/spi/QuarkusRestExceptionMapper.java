package io.quarkus.rest.runtime.spi;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import io.quarkus.rest.runtime.core.QuarkusRestRequestContext;

public interface QuarkusRestExceptionMapper<E extends Throwable> extends ExceptionMapper<E> {

    /**
     * Convenience method that allows for easy access to the request context
     */
    Response toResponse(E exception, QuarkusRestRequestContext context);
}
