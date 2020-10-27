package io.quarkus.rest.server.test.resource.basic.resource;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.QueryParam;

public interface ParameterSubResGenericInterface<T> {
    @GET
    String get(@QueryParam("foo") List<T> params);
}
