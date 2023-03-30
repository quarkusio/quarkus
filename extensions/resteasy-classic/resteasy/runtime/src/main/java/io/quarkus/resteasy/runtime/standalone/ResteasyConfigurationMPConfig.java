package io.quarkus.resteasy.runtime.standalone;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters;
import org.jboss.resteasy.spi.ResteasyConfiguration;

/**
 * Some resteasy components use this class for configuration. This bridges MP Config to ResteasyConfiguration
 *
 */
public class ResteasyConfigurationMPConfig implements ResteasyConfiguration {

    private static final Map<String, String> RESTEASY_QUARKUS_MAPPING_PARAMS = Map.of(
            ResteasyContextParameters.RESTEASY_GZIP_MAX_INPUT, "quarkus.resteasy.gzip.max-input");

    @Override
    public String getParameter(String name) {
        Config config = ConfigProvider.getConfig();
        if (config == null)
            return null;
        Optional<String> value = Optional.empty();
        String mappedProperty = RESTEASY_QUARKUS_MAPPING_PARAMS.get(name);
        if (mappedProperty != null) {
            // try to use quarkus parameter
            value = config.getOptionalValue(mappedProperty, String.class);
        }

        // if the parameter name is not mapped or there is no value, use the parameter name as provided
        return value.or(() -> config.getOptionalValue(name, String.class))
                .orElse(null);
    }

    @Override
    public Set<String> getParameterNames() {
        Config config = ConfigProvider.getConfig();
        if (config == null)
            return Collections.EMPTY_SET;
        HashSet<String> set = new HashSet<>();
        for (String name : config.getPropertyNames())
            set.add(name);
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
}
