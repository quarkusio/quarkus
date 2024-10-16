package io.quarkus.rest.client.reactive.runtime;

import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;
import org.jboss.resteasy.reactive.client.impl.RestClientRequestContext;

public interface ResteasyReactiveResponseExceptionMapper<T extends Throwable> extends ResponseExceptionMapper<T> {

    T toThrowable(Response response, RestClientRequestContext context);

    @Override
    default T toThrowable(Response response) {
        throw new IllegalStateException("should never be invoked");
    }
}
