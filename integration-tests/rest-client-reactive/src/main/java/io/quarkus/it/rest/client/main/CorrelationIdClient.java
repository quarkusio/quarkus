package io.quarkus.it.rest.client.main;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;

import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.smallrye.common.vertx.ContextLocals;

@Path("/correlation")
@RegisterRestClient(configKey = "correlation")
@RegisterProvider(CorrelationIdClient.CorrelationIdClientFilter.class)
public interface CorrelationIdClient {

    String CORRELATION_ID_HEADER_NAME = "CorrelationId";

    @GET
    String get();

    class CorrelationIdClientFilter implements ClientRequestFilter {

        @Override
        public void filter(ClientRequestContext requestContext) {
            String correlationId = ContextLocals.<String> get(CORRELATION_ID_HEADER_NAME).orElse(null);
            requestContext.getHeaders().putSingle(CORRELATION_ID_HEADER_NAME, correlationId);
        }
    }
}
