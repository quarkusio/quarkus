package io.quarkus.config.yaml.deployment;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/")
public class FooResource {

    @ConfigProperty(name = "foo.bar")
    String fooBar;

    @ConfigProperty(name = "foo.baz")
    String fooBaz;

    @Path("foo")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getBar() {
        return fooBar;
    }

    @Path("foo2")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getBaz() {
        return fooBaz;
    }
}
