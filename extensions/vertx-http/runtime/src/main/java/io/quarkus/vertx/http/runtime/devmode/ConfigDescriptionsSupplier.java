package io.quarkus.vertx.http.runtime.devmode;

import static io.smallrye.config.Expressions.withoutExpansion;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigSource;

import io.smallrye.config.ConfigValue;
import io.smallrye.config.SmallRyeConfig;

public class ConfigDescriptionsSupplier implements Supplier<Map<ConfigSourceName, List<ConfigDescription>>> {

    private List<ConfigDescription> configDescriptions;

    public ConfigDescriptionsSupplier() {
    }

    public ConfigDescriptionsSupplier(final List<ConfigDescription> configDescriptions) {
        this.configDescriptions = configDescriptions;
    }

    @Override
    public Map<ConfigSourceName, List<ConfigDescription>> get() {

        Map<ConfigSourceName, List<ConfigDescription>> ordered = new TreeMap<>();
        List<String> properties = new ArrayList<>();
        SmallRyeConfig current = (SmallRyeConfig) ConfigProvider.getConfig();

        for (ConfigDescription item : configDescriptions) {
            properties.add(item.getName());
            item.setConfigValue(current.getConfigValue(item.getName()));

            String configSourceName = item.getConfigValue().getConfigSourceName();
            int configSourceOrdinal = item.getConfigValue().getConfigSourceOrdinal();

            ordered.putIfAbsent(new ConfigSourceName(configSourceName, configSourceOrdinal), new ArrayList<>());
            ordered.get(new ConfigSourceName(configSourceName, configSourceOrdinal)).add(item);
        }

        for (ConfigSource configSource : current.getConfigSources()) {
            if (configSource.getName().equals("PropertiesConfigSource[source=Build system]")) {
                properties.addAll(configSource.getPropertyNames());
            }
        }

        withoutExpansion(() -> {
            for (String propertyName : current.getPropertyNames()) {
                if (properties.contains(propertyName)) {
                    continue;
                }

                ConfigDescription item = new ConfigDescription(propertyName, null, null, current.getConfigValue(propertyName));
                ConfigValue configValue = current.getConfigValue(propertyName);
                ConfigSourceName csn = new ConfigSourceName(configValue.getConfigSourceName(),
                        configValue.getConfigSourceOrdinal());
                ordered.putIfAbsent(csn, new ArrayList<>());
                ordered.get(csn).add(item);

                configDescriptions.add(item);
            }
        });

        return ordered;
    }

    public List<ConfigDescription> getConfigDescriptions() {
        return configDescriptions;
    }

    public void setConfigDescriptions(final List<ConfigDescription> configDescriptions) {
        this.configDescriptions = configDescriptions;
    }
}
