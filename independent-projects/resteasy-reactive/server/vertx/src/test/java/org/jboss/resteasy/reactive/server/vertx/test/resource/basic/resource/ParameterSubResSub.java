package org.jboss.resteasy.reactive.server.vertx.test.resource.basic.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Produces;

public interface ParameterSubResSub {
    @GET
    @Produces("text/plain")
    String get();
}
