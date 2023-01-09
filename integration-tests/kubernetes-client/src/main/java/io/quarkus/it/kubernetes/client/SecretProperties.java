package io.quarkus.it.kubernetes.client;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/secretProperties")
public class SecretProperties {

    @ConfigProperty(name = "dummysecret")
    String dummysecret;

    @ConfigProperty(name = "overridden.secret")
    String overriddenSecret;

    @ConfigProperty(name = "secret.prop1")
    String secretProp1;

    @ConfigProperty(name = "secret.prop2")
    String secretProp2;

    @ConfigProperty(name = "secret.prop3")
    String secretProp3;

    @ConfigProperty(name = "secret.prop4")
    String secretProp4;

    @GET
    @Path("/dummysecret")
    public String dummysecret() {
        return dummysecret;
    }

    @GET
    @Path("/overriddensecret")
    public String overriddensecret() {
        return overriddenSecret;
    }

    @GET
    @Path("/secretProp1")
    public String secretProp1() {
        return secretProp1;
    }

    @GET
    @Path("/secretProp2")
    public String secretProp2() {
        return secretProp2;
    }

    @GET
    @Path("/secretProp3")
    public String secretProp3() {
        return secretProp3;
    }

    @GET
    @Path("/secretProp4")
    public String secretProp4() {
        return secretProp4;
    }
}
