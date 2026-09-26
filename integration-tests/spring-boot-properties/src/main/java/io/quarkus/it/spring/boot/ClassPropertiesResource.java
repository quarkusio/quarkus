package io.quarkus.it.spring.boot;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/class")
public class ClassPropertiesResource {

    @Inject
    ClassProperties properties;

    @Path("/value")
    @GET
    public String getValue() {
        return properties.getValue();
    }

    @Path("/anotherClass/value")
    @GET
    public boolean isAnotherClassValue() {
        return properties.getAnotherClass().isValue();
    }
}
