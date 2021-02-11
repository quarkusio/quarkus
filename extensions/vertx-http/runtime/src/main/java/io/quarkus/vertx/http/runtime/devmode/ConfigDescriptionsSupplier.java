package io.quarkus.vertx.http.runtime.devmode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigSource;

import io.smallrye.config.SmallRyeConfig;

public class ConfigDescriptionsSupplier implements Supplier<List<ConfigDescription>> {
    private List<ConfigDescription> configDescriptions;

    public ConfigDescriptionsSupplier() {
    }

    public ConfigDescriptionsSupplier(final List<ConfigDescription> configDescriptions) {
        this.configDescriptions = configDescriptions;
    }

    @Override
    public List<ConfigDescription> get() {
        List<String> properties = new ArrayList<>();
        SmallRyeConfig current = (SmallRyeConfig) ConfigProvider.getConfig();

        for (ConfigDescription item : configDescriptions) {
            properties.add(item.getName());
            item.setConfigValue(current.getConfigValue(item.getName()));
        }

        for (ConfigSource configSource : current.getConfigSources()) {
            if (configSource.getName().equals("PropertiesConfigSource[source=Build system]")) {
                properties.addAll(configSource.getPropertyNames());
            }
        }

        for (String propertyName : current.getPropertyNames()) {
            if (properties.contains(propertyName)) {
                continue;
            }

            configDescriptions.add(new ConfigDescription(propertyName, null, null, current.getConfigValue(propertyName)));
        }

        Collections.sort(configDescriptions);
        return configDescriptions;
    }

    public List<ConfigDescription> getConfigDescriptions() {
        return configDescriptions;
    }

    public void setConfigDescriptions(final List<ConfigDescription> configDescriptions) {
        this.configDescriptions = configDescriptions;
    }
}
