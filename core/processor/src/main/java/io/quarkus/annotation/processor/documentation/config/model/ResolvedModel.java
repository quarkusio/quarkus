package io.quarkus.annotation.processor.documentation.config.model;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * This is the fully resolved model for a given module.
 * <p>
 * This model doesn't contain the Javadoc: the Javadoc is generated per module and can't be part of the model
 * as when referencing a ConfigGroup that is outside of the boundaries of the module, the Javadoc is not available.
 * <p>
 * The model is fully resolved though as all the config annotations have a runtime retention so, even if the source
 * is not available in the module, we can resolve all the annotations and the model.
 * <p>
 * It is the responsibility of the model consumer to assemble the config roots (if needed) and to get the Javadoc from the files
 * containing it.
 */
public class ResolvedModel {

    /**
     * List of config roots: note that at this point they are not merged: you have one object per {@code @ConfigRoot}
     * annotation.
     */
    private List<ConfigRoot> configRoots;

    @JsonCreator
    public ResolvedModel(List<ConfigRoot> configRoots) {
        this.configRoots = configRoots == null ? List.of() : Collections.unmodifiableList(configRoots);
    }

    public List<ConfigRoot> getConfigRoots() {
        return configRoots;
    }

    public void walk(ConfigItemVisitor visitor) {
        for (ConfigRoot configRoot : configRoots) {
            configRoot.walk(visitor);
        }
    }

    public boolean isEmpty() {
        return configRoots.isEmpty();
    }
}
