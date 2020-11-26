package io.quarkus.rest.server.test.resource.basic.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;

public interface ParameterSubResSub {
    @GET
    @Produces("text/plain")
    String get();
}
