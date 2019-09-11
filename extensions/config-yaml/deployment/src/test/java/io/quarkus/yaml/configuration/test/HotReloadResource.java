package io.quarkus.yaml.configuration.test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path(HotReloadResource.PATH)
public class HotReloadResource {

    public static final String PATH = "/hot-reload-test";

    @ConfigProperty(name = "config.value")
    String configValue;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getConfigValue() {
        return configValue;
    }
}
