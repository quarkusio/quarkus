package io.quarkus.it.spring.boot;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

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
