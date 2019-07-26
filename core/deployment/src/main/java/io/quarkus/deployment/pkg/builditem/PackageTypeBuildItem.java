package io.quarkus.deployment.pkg.builditem;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Build item that extensions must create to register a package type. This allows for verification
 * that a request package type can actually be produced
 */
public final class PackageTypeBuildItem extends MultiBuildItem {

    private final String type;

    public PackageTypeBuildItem(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
