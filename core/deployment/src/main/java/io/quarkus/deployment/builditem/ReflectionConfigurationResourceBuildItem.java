
package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Represents a configuration file for the serialization to be passed to native-image through the
 * {@code -H:ReflectionConfigurationResources} option.
 */
public final class ReflectionConfigurationResourceBuildItem extends MultiBuildItem {

    private final String file;

    public ReflectionConfigurationResourceBuildItem(String file) {
        this.file = file;
    }

    public String getFile() {
        return file;
    }
}
