package org.acme;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
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

    @GET
    @Path("disable")
    public void disable() {
        System.setProperty("quarkus.live-reload.instrumentation", "false");
    }

    @GET
    @Path("enable")
    public void enable() {
        System.setProperty("quarkus.live-reload.instrumentation","true");
    }
}
