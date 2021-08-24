package io.quarkus.rest.client.reactive.runtime;

import java.io.IOException;
import java.util.List;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;

import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;
import org.jboss.resteasy.reactive.client.handlers.ClientResponseCompleteRestHandler;
import org.jboss.resteasy.reactive.client.impl.ClientRequestContextImpl;
import org.jboss.resteasy.reactive.client.impl.RestClientRequestContext;
import org.jboss.resteasy.reactive.common.core.UnwrappableException;
import org.jboss.resteasy.reactive.common.jaxrs.ResponseImpl;

public class MicroProfileRestClientResponseFilter implements ClientResponseFilter {
    private final List<ResponseExceptionMapper<?>> exceptionMappers;

    public MicroProfileRestClientResponseFilter(List<ResponseExceptionMapper<?>> exceptionMappers) {
        if (exceptionMappers == null) {
            throw new NullPointerException("exceptionMappers cannot be null");
        }
        this.exceptionMappers = exceptionMappers;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) throws IOException {
        for (ResponseExceptionMapper exceptionMapper : exceptionMappers) {
            if (exceptionMapper.handles(responseContext.getStatus(), responseContext.getHeaders())) {
                // we have an exception mapper, we don't need the response anymore, we can map it to response right away (I hope :D)
                RestClientRequestContext restClientContext = ((ClientRequestContextImpl) requestContext)
                        .getRestClientRequestContext();
                ResponseImpl response = ClientResponseCompleteRestHandler.mapToResponse(restClientContext, false);
                Throwable throwable = exceptionMapper.toThrowable(response);
                if (throwable != null) {
                    throw new UnwrappableException(throwable);
                }
            }
        }
    }
}
