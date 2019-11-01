package io.quarkus.it.config;

import java.math.BigDecimal;

import javax.validation.constraints.Size;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import io.quarkus.arc.config.ConfigProperties;

@Path("/configuration-properties")
public class ConfigPropertiesResource {

    final GreetingConfiguration greetingConfiguration;

    public ConfigPropertiesResource(GreetingConfiguration greetingConfiguration) {
        this.greetingConfiguration = greetingConfiguration;
    }

    @GET
    public String greet() {
        return greetingConfiguration.message + greetingConfiguration.suffix;
    }

    @ConfigProperties(prefix = "configproperties")
    public static class GreetingConfiguration {
        @Size(min = 2)
        public String message;
        public String suffix = "!";
        public BigDecimal other;
    }
}
