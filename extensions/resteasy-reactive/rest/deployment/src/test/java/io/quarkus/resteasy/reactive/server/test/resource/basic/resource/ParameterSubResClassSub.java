package io.quarkus.resteasy.reactive.server.test.resource.basic.resource;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.UriInfo;

import org.junit.jupiter.api.Assertions;

import io.quarkus.arc.Unremovable;

@ApplicationScoped
@Unremovable
public class ParameterSubResClassSub {
    AtomicInteger resourceCounter = new AtomicInteger();
    @Inject
    ApplicationScopeObject appScope;

    @Inject
    RequestScopedObject requestScope;

    @GET
    @Produces("text/plain")
    public String get(@Context HttpHeaders headers, @Context UriInfo uriInfo) {
        Assertions.assertEquals("/path/subclass", uriInfo.getPath());
        Assertions.assertNotNull(headers.getHeaderString("Host"));
        return "resourceCounter:" + resourceCounter.incrementAndGet() + ",appscope:" + appScope.getCount() + ",requestScope:"
                + requestScope.getCount();
    }
}
