package org.jboss.resteasy.reactive.server.spi;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

/**
 * Allow ExceptionMapper classes that can use resume and suspend when attempting to convert
 * an exception into a response
 */
public interface ResteasyReactiveAsyncExceptionMapper<E extends Throwable> extends ExceptionMapper<E> {

    /**
     * If the handling of the exception involves async handling async results, the implementation can
     * use {@link AsyncExceptionMapperContext#suspend()} and {@link AsyncExceptionMapperContext#resume()}.
     *
     * In all cases, tt is the responsibility of the implementation to call
     * {@link AsyncExceptionMapperContext#setResponse(Response)}
     * when the exception has been properly mapped.
     *
     */
    void asyncResponse(E exception, AsyncExceptionMapperContext context);

    default Response toResponse(E exception) {
        throw new IllegalStateException("This should never have been called");
    }
}
