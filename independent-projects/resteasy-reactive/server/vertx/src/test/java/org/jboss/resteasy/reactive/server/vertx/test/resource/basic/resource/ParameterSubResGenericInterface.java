package org.jboss.resteasy.reactive.server.vertx.test.resource.basic.resource;

import java.util.List;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.QueryParam;

public interface ParameterSubResGenericInterface<T> {
    @GET
    String get(@QueryParam("foo") List<T> params);
}
