package org.acme;

import java.io.IOException;

import jakarta.annotation.Priority;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.ext.Provider;

@Provider
@Singleton
@Priority(Priorities.AUTHENTICATION)
public class CustomHeader2Filter implements ClientRequestFilter {

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        requestContext.getHeaders().add("CustomHeader2", "CustomValue2");
    }
}
