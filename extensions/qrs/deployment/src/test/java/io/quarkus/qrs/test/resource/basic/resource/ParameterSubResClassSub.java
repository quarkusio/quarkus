package io.quarkus.qrs.test.resource.basic.resource;

import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

import org.junit.jupiter.api.Assertions;

@ApplicationScoped
public class ParameterSubResClassSub {
    AtomicInteger resourceCounter = new AtomicInteger();
    @Inject
    ApplicationScopeObject appScope;

    @Inject
    RequestScopedObject requestScope;

    @Context
    UriInfo uriInfo;

    @GET
    @Produces("text/plain")
    public String get(@Context HttpHeaders headers) {
        Assertions.assertEquals("Wrong path value from injected UriInfo", "/path/subclass", uriInfo.getPath());
        Assertions.assertNotNull("Connection header from injected HttpHeaders is null", headers.getHeaderString("Connection"));
        return "resourceCounter:" + resourceCounter.incrementAndGet() + ",appscope:" + appScope.getCount() + ",requestScope:"
                + requestScope.getCount();
    }
}
