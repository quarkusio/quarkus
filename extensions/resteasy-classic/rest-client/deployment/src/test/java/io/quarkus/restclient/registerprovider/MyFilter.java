package io.quarkus.restclient.registerprovider;

import java.io.IOException;

import jakarta.inject.Inject;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;

import org.jboss.resteasy.core.ResteasyContext;
import org.junit.jupiter.api.Assertions;

import io.quarkus.arc.Arc;

public class MyFilter implements ClientRequestFilter {

    @Inject
    MethodsCollector collector;

    @Inject
    MyRequestBean requestBean;

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        if (requestContext.getUri().toString().contains("/called-from-client?")) {
            // make sure we have a request context
            Assertions.assertTrue(Arc.container().requestContext().isActive());
            // remember which request context for later examination
            collector.setRequestBeanFromFilter(requestBean.getUniqueNumber());
            // make sure we don't inherit the server's resteasy context
            Assertions.assertNull(ResteasyContext.getContextData(String.class));
            // add to the client context
            ResteasyContext.pushContext(Long.class, 42l);
        }
        collector.collect(requestContext.getMethod());
    }
}
