package org.jboss.resteasy.reactive.client.spi;

import java.io.IOException;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;

public interface ResteasyReactiveClientRequestFilter extends ClientRequestFilter {

    @Override
    default void filter(ClientRequestContext requestContext) throws IOException {
        filter((ResteasyReactiveClientRequestContext) requestContext);
    }

    void filter(ResteasyReactiveClientRequestContext requestContext);
}
