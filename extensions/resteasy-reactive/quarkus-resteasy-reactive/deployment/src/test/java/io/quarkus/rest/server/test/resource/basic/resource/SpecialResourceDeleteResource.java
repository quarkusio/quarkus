package io.quarkus.rest.server.test.resource.basic.resource;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.Path;

import org.junit.jupiter.api.Assertions;

@Path("/delete")
public class SpecialResourceDeleteResource {
    @DELETE
    @Consumes("text/plain")
    public void delete(String msg) {
        Assertions.assertEquals("hello", msg);
    }
}
