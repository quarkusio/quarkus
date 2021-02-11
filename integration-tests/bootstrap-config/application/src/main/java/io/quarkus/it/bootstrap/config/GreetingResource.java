package io.quarkus.it.bootstrap.config;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/greeting")
public class GreetingResource {

    @ConfigProperty(name = "foo.key.i2")
    String propertyFromBootstrapConfig;

    @GET
    public String greet() {
        return "hello " + propertyFromBootstrapConfig;
    }
}
