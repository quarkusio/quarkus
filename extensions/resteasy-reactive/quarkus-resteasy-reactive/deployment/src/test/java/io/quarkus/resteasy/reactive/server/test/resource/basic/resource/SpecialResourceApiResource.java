package io.quarkus.resteasy.reactive.server.test.resource.basic.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.Assertions;

@Path("/{api:(?i:api)}")
public class SpecialResourceApiResource {
    private static Logger LOG = Logger.getLogger(SpecialResourceApiResource.class);

    @Path("/{func:(?i:func)}")
    @GET
    @Produces("text/plain")
    public String func() {
        return "hello";
    }

    @PUT
    public void put(@Context HttpHeaders headers, String val) {
        LOG.debug(headers.getMediaType());
        Assertions.assertEquals("Wrong request content", val, "hello");
    }
}
