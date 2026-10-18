package io.quarkus.it.spring.boot;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/default")
public class DefaultPropertiesResource {

    @Inject
    DefaultProperties properties;

    @Path("/value")
    @GET
    public String getDefaultValue() {
        return properties.getValue();
    }
}
