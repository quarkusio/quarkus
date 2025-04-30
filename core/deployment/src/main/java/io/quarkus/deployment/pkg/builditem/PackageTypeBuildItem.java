package io.quarkus.deployment.pkg.builditem;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * @deprecated Ignored.
 */
@Deprecated(forRemoval = true)
public final class PackageTypeBuildItem extends MultiBuildItem {

    private final String type;

    public PackageTypeBuildItem(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
