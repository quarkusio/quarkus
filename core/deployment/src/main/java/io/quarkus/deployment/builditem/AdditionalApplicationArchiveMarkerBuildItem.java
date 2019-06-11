package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A marker file that if present indicates that a given archive should be treated as an
 * application archive
 */
public final class AdditionalApplicationArchiveMarkerBuildItem extends MultiBuildItem {

    private final String file;

    public AdditionalApplicationArchiveMarkerBuildItem(String file) {
        this.file = file;
    }

    public String getFile() {
        return file;
    }
}
