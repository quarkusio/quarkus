package io.quarkus.resteasy.runtime.standalone;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.resteasy.spi.ResteasyConfiguration;

/**
 * Some resteasy components use this class for configuration. This bridges MP Config to ResteasyConfiguration
 *
 */
public class ResteasyConfigurationMPConfig implements ResteasyConfiguration {
    @Override
    public String getParameter(String name) {
        Config config = ConfigProvider.getConfig();
        if (config == null)
            return null;
        return config.getOptionalValue(name, String.class).orElse(null);
    }

    @Override
    public Set<String> getParameterNames() {
        Config config = ConfigProvider.getConfig();
        if (config == null)
            return Collections.EMPTY_SET;
        HashSet<String> set = new HashSet<>();
        for (String name : config.getPropertyNames())
            set.add(name);
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
