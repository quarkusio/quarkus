package io.quarkus.it.spring.boot;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

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
