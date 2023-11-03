package io.quarkus.rest.client.reactive.provider;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import org.jboss.resteasy.reactive.client.spi.ResteasyReactiveClientRequestContext;
import org.jboss.resteasy.reactive.client.spi.ResteasyReactiveClientRequestFilter;

@Provider
public class GlobalRequestFilter implements ResteasyReactiveClientRequestFilter {
    public static final int STATUS = 233;

    public static boolean abort = false;

    @Override
    public void filter(ResteasyReactiveClientRequestContext requestContext) {
        if (abort) {
            requestContext.abortWith(Response.status(STATUS).build());
        }
    }
}
