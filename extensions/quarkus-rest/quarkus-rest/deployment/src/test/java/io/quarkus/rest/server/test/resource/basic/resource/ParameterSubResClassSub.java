package io.quarkus.rest.server.test.resource.basic.resource;

import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

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
