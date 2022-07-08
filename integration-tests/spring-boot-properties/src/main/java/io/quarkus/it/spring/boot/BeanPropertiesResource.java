package io.quarkus.it.spring.boot;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/bean")
public class BeanPropertiesResource {

    @Inject
    BeanProperties properties;

    @Path("/value")
    @GET
    public int getValue() {
        return properties.getValue();
    }

    @Path("/finalValue")
    @GET
    public String getFinalValue() {
        return properties.getFinalValue();
    }

    @Path("/packagePrivateValue")
    @GET
    public int getPackagePrivateValue() {
        return properties.packagePrivateValue;
    }

    @Path("/innerClass/value")
    @GET
    public String getInnerClassValue() {
        return properties.getInnerClass().getValue();
    }
}
