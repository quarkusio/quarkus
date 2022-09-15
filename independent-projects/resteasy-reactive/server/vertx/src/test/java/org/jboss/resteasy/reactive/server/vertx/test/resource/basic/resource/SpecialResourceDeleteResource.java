package org.jboss.resteasy.reactive.server.vertx.test.resource.basic.resource;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.Path;
import org.junit.jupiter.api.Assertions;

@Path("/delete")
public class SpecialResourceDeleteResource {
    @DELETE
    @Consumes("text/plain")
    public void delete(String msg) {
        Assertions.assertEquals("hello", msg);
    }
}
