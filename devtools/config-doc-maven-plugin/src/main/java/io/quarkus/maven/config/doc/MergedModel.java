package io.quarkus.maven.config.doc;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.quarkus.annotation.processor.documentation.config.model.ConfigRoot;
import io.quarkus.annotation.processor.documentation.config.model.ConfigSection;
import io.quarkus.annotation.processor.documentation.config.model.Extension;

public class MergedModel {

    private final Map<Extension, Map<String, ConfigRoot>> configRoots;

    private final Map<String, ConfigRoot> configRootsInSpecificFile;

    private final Map<Extension, List<ConfigSection>> generatedConfigSections;

    public MergedModel(Map<Extension, Map<String, ConfigRoot>> configRoots,
            Map<String, ConfigRoot> configRootsInSpecificFile,
            Map<Extension, List<ConfigSection>> configSections) {
        this.configRoots = Collections.unmodifiableMap(configRoots);
        this.configRootsInSpecificFile = Collections.unmodifiableMap(configRootsInSpecificFile);
        this.generatedConfigSections = Collections.unmodifiableMap(configSections);
    }

    public Map<Extension, Map<String, ConfigRoot>> getConfigRoots() {
        return configRoots;
    }

    public Map<String, ConfigRoot> getConfigRootsInSpecificFile() {
        return configRootsInSpecificFile;
    }

    public Map<Extension, List<ConfigSection>> getGeneratedConfigSections() {
        return generatedConfigSections;
    }
}
