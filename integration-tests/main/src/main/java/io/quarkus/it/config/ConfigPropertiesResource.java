package io.quarkus.it.config;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Optional;

import jakarta.validation.constraints.Size;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.quarkus.arc.config.ConfigProperties;

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
        return greetingConfiguration.message + greetingConfiguration.number + greetingConfiguration.suffix;
    }

    @GET
    @Path("/period")
    public String period() {
        return greetingConfiguration.period.get().toString();
    }

    @ConfigProperties(prefix = "configproperties")
    public static class GreetingConfiguration {
        @Size(min = 2)
        public String message;
        public String suffix = "!";
        public BigDecimal other;
        public NumberEnum number;
        // Force to use implicit converter to check for reflective registration
        public LocalDate date;
        public Optional<Period> period;
    }

    public enum NumberEnum {
        ONE,
        TWO;
    }

    @ConfigProperties(prefix = "configproperties")
    public interface GreetingConfigurationI {
        LocalDateTime dateTime();

        Optional<Instant> instant();
    }
}
