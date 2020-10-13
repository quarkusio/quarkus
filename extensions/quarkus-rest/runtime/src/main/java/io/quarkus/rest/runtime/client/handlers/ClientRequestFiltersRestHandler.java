package io.quarkus.rest.runtime.client.handlers;

import java.util.List;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.ClientRequestFilter;

import io.quarkus.rest.runtime.client.ClientRestHandler;
import io.quarkus.rest.runtime.client.QuarkusRestClientRequestContext;
import io.quarkus.rest.runtime.client.RestClientRequestContext;

public class ClientRequestFiltersRestHandler implements ClientRestHandler {
    @Override
    public void handle(RestClientRequestContext context) throws Exception {

        List<ClientRequestFilter> filters = context.getConfiguration().getRequestFilters();
        if (!filters.isEmpty()) {
            QuarkusRestClientRequestContext requestContext = context.getOrCreateClientRequestContext();
            for (ClientRequestFilter filter : filters) {
                try {
                    filter.filter(requestContext);
                } catch (Exception x) {
                    throw new ProcessingException(x);
                }
                if (requestContext.getAbortedWith() != null) {
                    context.setResponseStatus(requestContext.getAbortedWith().getStatus());
                    context.setResponseHeaders(requestContext.getAbortedWith().getStringHeaders());
                    context.setResponseReasonPhrase(requestContext.getAbortedWith().getStatusInfo().getReasonPhrase());
                    return;
                }
            }
        }
    }
}
