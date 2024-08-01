package io.quarkus.annotation.processor.documentation.config.model;

import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * This is the fully resolved model for a given module.
 * <p>
 * Note that while it's fully resolved at the module level, it might not be fully resolved in case a config group, an interface
 * or a superclass is in another module.
 * <p>
 * When assembling all the models at a more global level (for instance when generating the doc or in the IDE),
 * an additional resolution step might be needed if the model is not fully resolved.
 */
public class ResolvedModel {

    /**
     * Key is the prefix of the config root (all config roots with the same prefix are merged).
     */
    private Map<String, ConfigRoot> configRoots;

    /**
     * In some cases, we have a shared config mapping in a separate shared module.
     * <p>
     * These mappings are not resolved to config roots but might be useful when fully resolving the unresolved interfaces and
     * superclasses of a config root.
     * This is only useful for corner cases, for instance in the observability dev services.
     * <p>
     * Key is the prefix of the mapping.
     */
    // TODO implement unresolved config mappings
    //private Map<String, ConfigMapping> unresolvedConfigMappings;

    /**
     * Key is the qualified name of the class of the config group.
     */
    private Map<String, ConfigGroup> configGroups;

    @JsonCreator
    public ResolvedModel(Map<String, ConfigRoot> configRoots, Map<String, ConfigGroup> configGroups) {
        this.configRoots = configRoots == null ? Map.of() : Collections.unmodifiableMap(configRoots);
        this.configGroups = configGroups == null ? Map.of() : Collections.unmodifiableMap(configGroups);
    }

    public Map<String, ConfigRoot> getConfigRoots() {
        return configRoots;
    }

    public Map<String, ConfigGroup> getConfigGroups() {
        return configGroups;
    }
}
