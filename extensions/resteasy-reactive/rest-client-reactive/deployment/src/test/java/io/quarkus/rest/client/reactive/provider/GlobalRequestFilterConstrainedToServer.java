package io.quarkus.rest.client.reactive.provider;

import javax.ws.rs.ConstrainedTo;
import javax.ws.rs.RuntimeType;
import javax.ws.rs.ext.Provider;

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
