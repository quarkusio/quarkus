package org.jboss.resteasy.reactive.server.vertx.test.resource.basic.resource;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;

public interface SubResourceLocatorFoo<T> {
    @GET
    T getFoo(@HeaderParam("foo") String val);
}
