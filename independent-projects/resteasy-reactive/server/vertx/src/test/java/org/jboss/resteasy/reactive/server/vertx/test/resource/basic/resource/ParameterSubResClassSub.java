package org.jboss.resteasy.reactive.server.vertx.test.resource.basic.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.Assertions;

public class ParameterSubResClassSub {
    @Context
    UriInfo uriInfo;

    @GET
    @Produces("text/plain")
    public String get(@Context HttpHeaders headers) {
        Assertions.assertEquals("/path/subclass", uriInfo.getPath());
        Assertions.assertNotNull(headers.getHeaderString("Host"));
        return uriInfo.getPath();
    }
}
