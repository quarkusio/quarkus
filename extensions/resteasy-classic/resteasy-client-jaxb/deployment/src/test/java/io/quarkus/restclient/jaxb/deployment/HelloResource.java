package io.quarkus.restclient.jaxb.deployment;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Produces(MediaType.APPLICATION_XML)
@Path("/hello")
public class HelloResource {

    @GET
    public String hello() {
        return "<book><title>L'axe du loup</title></book>";
    }
}
