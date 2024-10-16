package io.quarkus.annotation.processor.documentation.config.merger;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.quarkus.annotation.processor.documentation.config.model.ConfigRoot;
import io.quarkus.annotation.processor.documentation.config.model.ConfigSection;
import io.quarkus.annotation.processor.documentation.config.model.Extension;

/**
 * The merged model we obtain after merging all the ResolvedModels from the current project.
 */
public class MergedModel {

    private final Map<Extension, Map<ConfigRootKey, ConfigRoot>> configRoots;

    private final Map<String, ConfigRoot> configRootsInSpecificFile;

    private final Map<Extension, List<ConfigSection>> generatedConfigSections;

    MergedModel(Map<Extension, Map<ConfigRootKey, ConfigRoot>> configRoots,
            Map<String, ConfigRoot> configRootsInSpecificFile,
            Map<Extension, List<ConfigSection>> configSections) {
        this.configRoots = Collections.unmodifiableMap(configRoots);
        this.configRootsInSpecificFile = Collections.unmodifiableMap(configRootsInSpecificFile);
        this.generatedConfigSections = Collections.unmodifiableMap(configSections);
    }

    public Map<Extension, Map<ConfigRootKey, ConfigRoot>> getConfigRoots() {
        return configRoots;
    }

    public Map<String, ConfigRoot> getConfigRootsInSpecificFile() {
        return configRootsInSpecificFile;
    }

    public Map<Extension, List<ConfigSection>> getGeneratedConfigSections() {
        return generatedConfigSections;
    }

    public boolean isEmpty() {
        return configRoots.isEmpty();
    }

    public record ConfigRootKey(String topLevelPrefix, String description) implements Comparable<ConfigRootKey> {

        @Override
        public final String toString() {
            return topLevelPrefix;
        }

        @Override
        public int compareTo(ConfigRootKey other) {
            int compareTopLevelPrefix = this.topLevelPrefix.compareToIgnoreCase(other.topLevelPrefix);
            if (compareTopLevelPrefix != 0) {
                return compareTopLevelPrefix;
            }
            if (this.description == null && other.description == null) {
                return 0;
            }
            if (this.description == null) {
                return -1;
            }
            if (other.description == null) {
                return 1;
            }

            return this.description.compareToIgnoreCase(other.description);
        }
    }
}
