package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A build item that represents an additional config file that will be automatically created in the
 * fast-jar config directory.
 */
public final class AdditionalRuntimeConfigFileBuildItem extends MultiBuildItem {

    private final String fileName;
    private final String defaultContents;

    public AdditionalRuntimeConfigFileBuildItem(String fileName, String defaultContents) {
        this.fileName = fileName;
        this.defaultContents = defaultContents;
    }

    public String getDefaultContents() {
        return defaultContents;
    }

    public String getFileName() {
        return fileName;
    }
}
