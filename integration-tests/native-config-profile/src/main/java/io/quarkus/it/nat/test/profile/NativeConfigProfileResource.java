package io.quarkus.it.nat.test.profile;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

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
