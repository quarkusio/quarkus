package io.quarkus.restclient.jaxb.deployment;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Produces(MediaType.APPLICATION_XML)
@Path("/hello")
public class HelloResource {

    @GET
    public String hello() {
        return "<book><title>L'axe du loup</title></book>";
    }
}
