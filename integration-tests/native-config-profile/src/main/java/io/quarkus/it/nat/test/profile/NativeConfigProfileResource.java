package io.quarkus.it.nat.test.profile;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.arc.Arc;

@Path("/native-config-profile")
public class NativeConfigProfileResource {

    @ConfigProperty(name = "my.config.value")
    String myConfigValue;

    @Path("/myConfigValue")
    @Produces("text/plain")
    @GET
    public String myConfigValue() {
        return myConfigValue;
    }

    @Path("/unused-exists")
    @Produces("text/plain")
    @GET
    public boolean unusedExists() {
        return Arc.container().instance(UnusedRemovableBean.class).isAvailable();
    }

}
