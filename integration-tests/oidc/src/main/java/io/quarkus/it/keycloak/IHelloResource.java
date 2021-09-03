package io.quarkus.it.keycloak;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/hello")
public interface IHelloResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    String hello();
}
