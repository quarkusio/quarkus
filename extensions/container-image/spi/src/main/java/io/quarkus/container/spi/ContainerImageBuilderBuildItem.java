
package io.quarkus.container.spi;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A {@link MultiBuildItem} that represents eligible container image builders. Some extension have a dependency on
 * external services (e.g. openshift and s2i). So, the presence of the extension alone is not enough to let the build
 * system know that extension is usable. This build item is produced only when all environment requirements are met.
 */
public final class ContainerImageBuilderBuildItem extends MultiBuildItem {

    private final String builder;

    public ContainerImageBuilderBuildItem(String builder) {
        this.builder = builder;
    }

    public String getBuilder() {
        return builder;
    }
}
