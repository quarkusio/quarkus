package io.quarkus.resteasy.runtime.standalone;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters;
import org.jboss.resteasy.spi.ResteasyConfiguration;

import io.quarkus.runtime.configuration.MemorySize;

/**
 * Some RESTEasy components use this class for configuration. This bridges MP Config to ResteasyConfiguration
 *
 */
public class ResteasyConfigurationMPConfig implements ResteasyConfiguration {

    private static final Map<String, Function<Config, Optional<String>>> RESTEASY_QUARKUS_MAPPING_PARAMS = Map.of(
            ResteasyContextParameters.RESTEASY_GZIP_MAX_INPUT, ResteasyConfigurationMPConfig::getGzipMaxInput);

    @Override
    public String getParameter(String name) {
        Config config = ConfigProvider.getConfig();
        if (config == null) {
            return null;
        }

        Optional<String> value = Optional.empty();
        Function<Config, Optional<String>> mappingFunction = RESTEASY_QUARKUS_MAPPING_PARAMS.get(name);
        if (mappingFunction != null) {
            // try to use Quarkus configuration
            value = mappingFunction.apply(config);
        }

        // if the parameter name is not mapped or there is no value, use the parameter name as provided
        return value.or(() -> config.getOptionalValue(name, String.class))
                .orElse(null);
    }

    @Override
    public Set<String> getParameterNames() {
        Config config = ConfigProvider.getConfig();
        if (config == null) {
            return Set.of();
        }
        HashSet<String> set = new HashSet<>();
        for (String name : config.getPropertyNames()) {
            set.add(name);
        }
        set.addAll(RESTEASY_QUARKUS_MAPPING_PARAMS.keySet());

        return set;
    }

    @Override
    public String getInitParameter(String name) {
        return getParameter(name);
    }

    @Override
    public Set<String> getInitParameterNames() {
        return getParameterNames();
    }

    private static Optional<String> getGzipMaxInput(Config config) {
        if (config.getOptionalValue("resteasy.gzip.max.input", String.class).isPresent()) {
            // resteasy-specific properties have priority
            return Optional.empty();
        }

        Optional<MemorySize> rawValue = config.getOptionalValue("quarkus.resteasy.gzip.max-input", MemorySize.class);

        if (rawValue.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(Long.toString(rawValue.get().asLongValue()));
    }
}
