package io.quarkus.resteasy.reactive.server.test.customproviders;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;

import org.jboss.resteasy.reactive.server.ServerRequestFilter;
import org.jboss.resteasy.reactive.server.ServerResponseFilter;

public class MetalFilter {

    @Metal
    @ServerRequestFilter
    public void headBangIn(ContainerRequestContext requestContext) {
        requestContext.getHeaders().putSingle("heavy", "metal");
    }

    @Metal
    @ServerResponseFilter
    public void headBangOut(ContainerResponseContext responseContext) {
        responseContext.getHeaders().putSingle("very", "heavy");
    }
}
