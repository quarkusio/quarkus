package io.quarkus.it.resteasy.jackson;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;

import org.jboss.resteasy.spi.ResteasyConfiguration;

@Path("/greeting")
public class GreetingResource {

    @POST
    public Greeting hello(Greeting body) {
        return body;
    }

    @Path("config")
    @GET
    @Produces("text/plain")
    public String config(@Context ResteasyConfiguration config) {
        // test that configuration can be obtained from application.properties
        return config.getParameter("resteasy.gzip.max.input");
    }
}
