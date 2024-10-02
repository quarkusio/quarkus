package io.quarkus.rest.client.reactive.runtime;

import static org.jboss.resteasy.reactive.client.impl.RestClientRequestContext.INVOKED_EXCEPTION_MAPPER_CLASS_NAME_PROP;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;

import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;
import org.jboss.resteasy.reactive.client.handlers.ClientResponseCompleteRestHandler;
import org.jboss.resteasy.reactive.client.impl.ClientRequestContextImpl;
import org.jboss.resteasy.reactive.client.impl.RestClientRequestContext;
import org.jboss.resteasy.reactive.client.spi.ClientRestHandler;
import org.jboss.resteasy.reactive.common.core.UnwrappableException;
import org.jboss.resteasy.reactive.common.jaxrs.ResponseImpl;

import io.vertx.core.Context;

public class MicroProfileRestClientResponseFilter implements ClientResponseFilter {
    private static final ClientRestHandler[] EMPTY_CLIENT_REST_HANDLERS = new ClientRestHandler[0];
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
                RestClientRequestContext restClientContext = ((ClientRequestContextImpl) requestContext)
                        .getRestClientRequestContext();

                boolean requiresBlocking = RestClientRecorder.isClassBlocking(exceptionMapper.getClass());
                if (Context.isOnEventLoopThread() && requiresBlocking) {
                    switchToWorkerThreadPoolAndRetry(restClientContext);
                    break;
                } else {
                    // we have an exception mapper, we don't need the response anymore, we can map it to response right away (I hope :D)
                    ResponseImpl response = ClientResponseCompleteRestHandler.mapToResponse(restClientContext, false);
                    Throwable throwable;
                    if (exceptionMapper instanceof ResteasyReactiveResponseExceptionMapper) {
                        throwable = ((ResteasyReactiveResponseExceptionMapper) exceptionMapper).toThrowable(response,
                                restClientContext);
                    } else {
                        throwable = exceptionMapper.toThrowable(response);
                    }
                    requestContext.setProperty(INVOKED_EXCEPTION_MAPPER_CLASS_NAME_PROP, exceptionMapper.getClass().getName());
                    if (throwable != null) {
                        throw new UnwrappableException(throwable);
                    }
                }
            }
        }
    }

    private void switchToWorkerThreadPoolAndRetry(RestClientRequestContext restClientContext) {
        int position = restClientContext.getPosition();

        List<ClientRestHandler> nextHandlers = new ArrayList<>(2 + restClientContext.getHandlers().length - position);
        nextHandlers.add(new ClientUseWorkerExecutorRestHandler());
        nextHandlers.add(currentHandler(restClientContext));

        while (position < restClientContext.getHandlers().length) {
            nextHandlers.add(restClientContext.getHandlers()[position]);
            position++;
        }

        restClientContext.restart(nextHandlers.toArray(EMPTY_CLIENT_REST_HANDLERS), true);
    }

    private ClientRestHandler currentHandler(RestClientRequestContext restClientContext) {
        return restClientContext.getHandlers()[restClientContext.getPosition() - 1];
    }
}
