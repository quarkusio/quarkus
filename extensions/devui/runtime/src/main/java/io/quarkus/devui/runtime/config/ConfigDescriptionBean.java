package io.quarkus.devui.runtime.config;

import java.util.List;
import java.util.Map;

import io.quarkus.vertx.http.runtime.devmode.ConfigDescription;

public class ConfigDescriptionBean {
    private List<ConfigDescription> allConfig;
    private Map<String, List<String>> extensionConfigFilters;

    public ConfigDescriptionBean(final List<ConfigDescription> allConfig,
            final Map<String, List<String>> extensionConfigFilters) {
        this.allConfig = allConfig;
        this.extensionConfigFilters = extensionConfigFilters;
    }

    public List<ConfigDescription> getAllConfig() {
        return allConfig;
    }

    public Map<String, List<String>> getExtensionConfigFilters() {
        return extensionConfigFilters;
    }
}
