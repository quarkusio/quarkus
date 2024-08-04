package io.quarkus.maven.config.doc;

import java.util.List;
import java.util.Map;

import io.quarkus.annotation.processor.documentation.config.model.ConfigRoot;
import io.quarkus.annotation.processor.documentation.config.model.ConfigSection;
import io.quarkus.annotation.processor.documentation.config.model.Extension;

public class MergedModel {

    private final Map<Extension, Map<String, ConfigRoot>> configRoots;

    private final Map<Extension, List<ConfigSection>> generatedConfigSections;

    public MergedModel(Map<Extension, Map<String, ConfigRoot>> configRoots,
            Map<Extension, List<ConfigSection>> configSections) {
        this.configRoots = configRoots;
        this.generatedConfigSections = configSections;
    }

    public Map<Extension, Map<String, ConfigRoot>> getConfigRoots() {
        return configRoots;
    }

    public Map<Extension, List<ConfigSection>> getGeneratedConfigSections() {
        return generatedConfigSections;
    }
}
