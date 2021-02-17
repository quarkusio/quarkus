package io.quarkus.resteasy.reactive.server.test.customproviders;

import javax.ws.rs.container.ContainerRequestContext;

import org.jboss.resteasy.reactive.server.ServerRequestFilter;

public class MetalFilter {

    @Metal
    @ServerRequestFilter
    public void headBang(ContainerRequestContext requestContext) {
        requestContext.getHeaders().putSingle("heavy", "metal");
    }
}
