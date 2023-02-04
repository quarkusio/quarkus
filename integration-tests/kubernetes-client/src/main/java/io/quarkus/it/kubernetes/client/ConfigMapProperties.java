package io.quarkus.it.kubernetes.client;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.fabric8.kubernetes.client.utils.Utils;

@Path("/configMapProperties")
public class ConfigMapProperties {

    static {
        // this is only done to ensure that Utils is reachable
        // see https://github.com/quarkusio/quarkus/issues/21398
        String osName = Utils.OS_NAME;
        System.out.println(osName);
    }

    @ConfigProperty(name = "dummy")
    String dummy;

    @ConfigProperty(name = "some.prop1")
    String someProp1;

    @ConfigProperty(name = "some.prop2")
    String someProp2;

    @ConfigProperty(name = "some.prop3")
    String someProp3;

    @ConfigProperty(name = "some.prop4")
    String someProp4;

    @ConfigProperty(name = "some.prop5")
    String someProp5;

    @GET
    @Path("/dummy")
    public String dummy() {
        return dummy;
    }

    @GET
    @Path("/someProp1")
    public String someProp1() {
        return someProp1;
    }

    @GET
    @Path("/someProp2")
    public String someProp2() {
        return someProp2;
    }

    @GET
    @Path("/someProp3")
    public String someProp3() {
        return someProp3;
    }

    @GET
    @Path("/someProp4")
    public String someProp4() {
        return someProp4;
    }

    @GET
    @Path("/someProp5")
    public String someProp5() {
        return someProp5;
    }
}
