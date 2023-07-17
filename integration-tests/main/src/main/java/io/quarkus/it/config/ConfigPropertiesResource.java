package io.quarkus.it.config;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Optional;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@Path("/configuration-properties")
public class ConfigPropertiesResource {

    final GreetingConfiguration greetingConfiguration;
    final GreetingConfigurationI greetingConfigurationI;

    public ConfigPropertiesResource(GreetingConfiguration greetingConfiguration,
            GreetingConfigurationI greetingConfigurationI) {
        this.greetingConfiguration = greetingConfiguration;
        this.greetingConfigurationI = greetingConfigurationI;
    }

    @GET
    public String greet() {
        return greetingConfiguration.message() + greetingConfiguration.number() + greetingConfiguration.suffix();
    }

    @GET
    @Path("/period")
    public String period() {
        return greetingConfiguration.period().get().toString();
    }

    @ConfigMapping(prefix = "configproperties")
    public interface GreetingConfiguration {
        String message();

        @WithDefault("!")
        String suffix();

        BigDecimal other();

        NumberEnum number();

        // Force to use implicit converter to check for reflective registration
        LocalDate date();

        Optional<Period> period();
    }

    public enum NumberEnum {
        ONE,
        TWO;
    }

    @ConfigMapping(prefix = "configproperties")
    public interface GreetingConfigurationI {
        LocalDateTime dateTime();

        Optional<Instant> instant();
    }
}
