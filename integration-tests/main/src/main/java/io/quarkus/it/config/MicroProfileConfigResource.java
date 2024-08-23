package io.quarkus.it.config;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.wildfly.common.net.CidrAddress;

/**
 * Test some MicroProfile config primitives.
 * <p>
 * It needs to be there as RESTEasy recently broke MicroProfile Config
 * so we want to test this with RESTEasy in the classpath.
 */
@Path("/microprofile-config")
public class MicroProfileConfigResource {

    @ConfigProperty(name = "microprofile.custom.value")
    MicroProfileCustomValue value;

    @ConfigProperty(name = "microprofile.cidr-address")
    CidrAddress cidrAddress;

    @Inject
    Config config;

    @GET
    @Path("/get-property-names")
    public String getPropertyNames() throws Exception {
        if (!config.getPropertyNames().iterator().hasNext()) {
            return "No config property found. Some were expected.";
        }
        return "OK";
    }

    @GET
    @Path("/get-custom-value")
    public String getCustomValue() {
        return Integer.toString(value.getNumber());
    }

    @GET
    @Path("/get-cidr-address")
    public String getCidrAddress() {
        return cidrAddress.getNetworkAddress().getHostAddress();
    }
}
