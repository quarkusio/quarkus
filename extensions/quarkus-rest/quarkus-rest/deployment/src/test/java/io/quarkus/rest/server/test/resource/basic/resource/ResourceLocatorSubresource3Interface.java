package io.quarkus.rest.server.test.resource.basic.resource;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

public interface ResourceLocatorSubresource3Interface {

    @GET
    @Path("3")
    String get(@QueryParam("foo") List<Double> params);
}
