package io.quarkus.it.rest.client.main;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;

public class DefaultCtorTestFilter implements ClientRequestFilter {

    @Override
    public void filter(ClientRequestContext requestContext) {
        // Do nothing on purpose.
    }
}
