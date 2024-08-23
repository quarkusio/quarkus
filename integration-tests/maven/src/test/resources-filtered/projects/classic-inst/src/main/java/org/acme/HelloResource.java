package org.acme;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.UUID;

@Path("/app")
public class HelloResource {

    static final UUID uuid;

    static {
        uuid = UUID.randomUUID();
    }

    private final HelloService resource;

    @Inject
    public HelloResource(HelloService resource) {
        this.resource = resource;
    }

    @GET
    @Path("hello")
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "hello";
    }

    @GET
    @Path("name")
    @Produces(MediaType.TEXT_PLAIN)
    public String name() {
        return "hello " + resource.name();
    }

    @GET
    @Path("uuid")
    @Produces(MediaType.TEXT_PLAIN)
    public String uuid() {
        return uuid.toString();
    }

}
