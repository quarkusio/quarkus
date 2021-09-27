package io.quarkus.it.rest.client.main;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;

public class DefaultCtorTestFilter implements ClientRequestFilter {

    @Override
    public void filter(ClientRequestContext requestContext) {
        System.out.println(requestContext.getMethod());
    }
}
