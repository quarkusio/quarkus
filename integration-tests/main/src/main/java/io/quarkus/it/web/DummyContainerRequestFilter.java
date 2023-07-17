package io.quarkus.it.web;

import java.util.Objects;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.ext.Provider;

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
