package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Represents a configuration file for the serialization to be passed to native-image through the
 * {@code -H:SerializationConfigurationResources} option.
 */
public final class SerializationConfigurationResourceBuildItem extends MultiBuildItem {

    private final String file;

    public SerializationConfigurationResourceBuildItem(String file) {
        this.file = file;
    }

    public String getFile() {
        return file;
    }
}
