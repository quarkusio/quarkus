package org.jboss.resteasy.reactive.server.vertx.test.resource.basic.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import java.util.List;

public interface ResourceLocatorSubresource3Interface {

    @GET
    @Path("3")
    String get(@QueryParam("foo") List<Double> params);
}
