package io.quarkus.config.yaml.deployment;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
