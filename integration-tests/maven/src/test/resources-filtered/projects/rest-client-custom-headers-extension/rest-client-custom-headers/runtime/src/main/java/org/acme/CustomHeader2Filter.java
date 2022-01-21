package org.acme;

import java.io.IOException;

import javax.annotation.Priority;
import javax.inject.Singleton;
import javax.ws.rs.Priorities;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.ext.Provider;

@Provider
@Singleton
@Priority(Priorities.AUTHENTICATION)
public class CustomHeader2Filter implements ClientRequestFilter {

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        requestContext.getHeaders().add("CustomHeader2", "CustomValue2");
    }
}
