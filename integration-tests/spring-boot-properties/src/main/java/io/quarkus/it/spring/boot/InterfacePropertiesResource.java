package io.quarkus.it.spring.boot;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/interface")
public class InterfacePropertiesResource {

    @Inject
    InterfaceProperties properties;

    @Path("/value")
    @GET
    public String getValue() {
        return properties.getValue();
    }
}
