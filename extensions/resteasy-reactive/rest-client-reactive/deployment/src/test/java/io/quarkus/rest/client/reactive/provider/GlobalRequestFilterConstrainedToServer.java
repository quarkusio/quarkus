package io.quarkus.rest.client.reactive.provider;

import jakarta.ws.rs.ConstrainedTo;
import jakarta.ws.rs.RuntimeType;
import jakarta.ws.rs.ext.Provider;

import org.jboss.resteasy.reactive.client.spi.ResteasyReactiveClientRequestContext;
import org.jboss.resteasy.reactive.client.spi.ResteasyReactiveClientRequestFilter;

@Provider
@ConstrainedTo(RuntimeType.SERVER)
public class GlobalRequestFilterConstrainedToServer implements ResteasyReactiveClientRequestFilter {
    @Override
    public void filter(ResteasyReactiveClientRequestContext requestContext) {
        throw new RuntimeException("Invoked filter that is constrained to server");
    }
}
