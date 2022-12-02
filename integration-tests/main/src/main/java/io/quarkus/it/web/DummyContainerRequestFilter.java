package io.quarkus.it.web;

import java.util.Objects;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

import org.jboss.resteasy.spi.HttpRequest;

@Provider
@PreMatching
public class DummyContainerRequestFilter implements ContainerRequestFilter {

    @Context
    private HttpRequest request;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        Objects.requireNonNull(requestContext);
    }
}
