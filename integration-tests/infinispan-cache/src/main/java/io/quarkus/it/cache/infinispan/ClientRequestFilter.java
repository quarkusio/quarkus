package io.quarkus.it.cache.infinispan;

import jakarta.inject.Inject;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ClientRequestFilter implements jakarta.ws.rs.client.ClientRequestFilter {

    ClientRequestService requestService;

    @Inject
    public ClientRequestFilter(ClientRequestService requestService) {
        this.requestService = requestService;
    }

    @Override
    public void filter(ClientRequestContext requestContext) {
        if (requestService != null && requestService.data() != null) {
            requestContext.getHeaders().add("extra", requestService.data());
        }
    }
}