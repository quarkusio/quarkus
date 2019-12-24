package io.quarkus.it.spring.boot;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

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
