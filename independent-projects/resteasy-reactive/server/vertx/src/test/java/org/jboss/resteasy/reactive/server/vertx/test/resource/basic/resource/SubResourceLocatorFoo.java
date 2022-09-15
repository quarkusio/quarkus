package org.jboss.resteasy.reactive.server.vertx.test.resource.basic.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;

public interface SubResourceLocatorFoo<T> {
    @GET
    T getFoo(@HeaderParam("foo") String val);
}
