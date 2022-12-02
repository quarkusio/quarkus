package io.quarkus.rest.client.reactive.provider;

import javax.annotation.Priority;
import javax.ws.rs.client.ClientResponseContext;

import org.jboss.resteasy.reactive.client.spi.ResteasyReactiveClientRequestContext;
import org.jboss.resteasy.reactive.client.spi.ResteasyReactiveClientResponseFilter;

@Priority(1)
public class ResponseFilterLowestPrio implements ResteasyReactiveClientResponseFilter {

    public static final int STATUS = 266;
    public static boolean skip = false;

    @Override
    public void filter(ResteasyReactiveClientRequestContext requestContext, ClientResponseContext responseContext) {
        if (!skip) {
            responseContext.setStatus(STATUS);
        }
    }
}
