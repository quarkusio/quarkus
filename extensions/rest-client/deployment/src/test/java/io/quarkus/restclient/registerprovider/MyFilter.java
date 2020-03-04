package io.quarkus.restclient.registerprovider;

import java.io.IOException;

import javax.inject.Inject;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;

public class MyFilter implements ClientRequestFilter {

    @Inject
    MethodsCollector collector;

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        collector.collect(requestContext.getMethod());
    }
}
