package org.jboss.resteasy.reactive.server.vertx.test.customproviders;

import jakarta.ws.rs.container.ContainerRequestContext;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;

public class MetalFilter {

    @Metal
    @ServerRequestFilter
    public void headBang(ContainerRequestContext requestContext) {
        requestContext.getHeaders().putSingle("heavy", "metal");
    }
}
