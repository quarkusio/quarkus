package io.quarkus.vertx.http.runtime.devmode;

import static io.smallrye.config.Expressions.withoutExpansion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

        Map<String, List<ConfigDescription>> mappedBySource = new HashMap<>();

        List<String> properties = new ArrayList<>();
        SmallRyeConfig current = (SmallRyeConfig) ConfigProvider.getConfig();

        for (ConfigDescription item : configDescriptions) {
            properties.add(item.getName());
            item.setConfigValue(current.getConfigValue(item.getName()));

            String configSourceName = item.getConfigValue().getConfigSourceName();

            mappedBySource.putIfAbsent(configSourceName, new ArrayList<>());
            mappedBySource.get(configSourceName).add(item);
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
                String configSourceName = current.getConfigValue(propertyName).getConfigSourceName();
                mappedBySource.putIfAbsent(configSourceName, new ArrayList<>());
                mappedBySource.get(configSourceName).add(item);

                configDescriptions.add(item);
            }
        });

        List<ConfigDescription> orderedBySource = new ArrayList<>();

        // PropertiesConfigSource[source=application.properties]
        if (mappedBySource.containsKey(APPLICATION_PROPERTIES_CONFIG_SOURCE)) {
            List<ConfigDescription> applicationProperties = mappedBySource.remove(APPLICATION_PROPERTIES_CONFIG_SOURCE);
            Collections.sort(applicationProperties);
            orderedBySource.addAll(applicationProperties);
        }

        // All other PropertiesConfigSource
        List<String> otherPropertySources = getOtherPropertiesConfigSources(mappedBySource.keySet());
        for (String name : otherPropertySources) {
            List<ConfigDescription> other = mappedBySource.remove(name);
            Collections.sort(other);
            orderedBySource.addAll(other);
        }

        // default value
        if (mappedBySource.containsKey(DEFAULT_VALUES)) {
            List<ConfigDescription> p = mappedBySource.remove(DEFAULT_VALUES);
            Collections.sort(p);
            orderedBySource.addAll(p);
        }
        // null 
        if (mappedBySource.containsKey(null)) {
            List<ConfigDescription> p = mappedBySource.remove(null);
            Collections.sort(p);
            orderedBySource.addAll(p);
        }

        // Now the rest
        Set<Map.Entry<String, List<ConfigDescription>>> entrySet = mappedBySource.entrySet();
        for (Map.Entry<String, List<ConfigDescription>> e : entrySet) {
            List<ConfigDescription> p = e.getValue();
            Collections.sort(p);
            orderedBySource.addAll(p);
        }

        return orderedBySource;
    }

    public List<ConfigDescription> getConfigDescriptions() {
        return configDescriptions;
    }

    public void setConfigDescriptions(final List<ConfigDescription> configDescriptions) {
        this.configDescriptions = configDescriptions;
    }

    private List<String> getOtherPropertiesConfigSources(Set<String> allNames) {
        List<String> l = new ArrayList<>();
        for (String name : allNames) {
            if (name != null && name.startsWith(PROPERTIES_CONFIG_SOURCE)) {
                l.add(name);
            }
        }
        return l;
    }

    private static final String APPLICATION_PROPERTIES_CONFIG_SOURCE = "PropertiesConfigSource[source=application.properties]";
    private static final String PROPERTIES_CONFIG_SOURCE = "PropertiesConfigSource";
    private static final String DEFAULT_VALUES = "default values";

}
