package org.jboss.resteasy.reactive.server.vertx.test.resource.basic.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;
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
