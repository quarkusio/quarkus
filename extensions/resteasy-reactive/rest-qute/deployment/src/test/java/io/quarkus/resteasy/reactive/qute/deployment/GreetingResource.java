package io.quarkus.resteasy.reactive.qute.deployment;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.quarkus.qute.i18n.LocaleAware;

@Path("greeting")
public class GreetingResource {

    @LocaleAware
    GreetingMessages messages;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return messages.hello();
    }

    @GET
    @Path("{name}")
    @Produces(MediaType.TEXT_PLAIN)
    public String helloName(@PathParam("name") String name) {
        return messages.hello_name(name);
    }

}
