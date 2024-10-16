package io.quarkus.devui.runtime.config;

import java.util.List;

public class ConfigDescriptionBean {
    private List<ConfigDescription> allConfig;

    public ConfigDescriptionBean(final List<ConfigDescription> allConfig) {
        this.allConfig = allConfig;
    }

    public List<ConfigDescription> getAllConfig() {
        return allConfig;
    }
}
