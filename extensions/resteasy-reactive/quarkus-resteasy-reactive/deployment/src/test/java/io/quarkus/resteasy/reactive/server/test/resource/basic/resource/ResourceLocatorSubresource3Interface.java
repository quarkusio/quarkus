package io.quarkus.resteasy.reactive.server.test.resource.basic.resource;

import java.util.List;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

public interface ResourceLocatorSubresource3Interface {

    @GET
    @Path("3")
    String get(@QueryParam("foo") List<Double> params);
}
