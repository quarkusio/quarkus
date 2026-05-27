package io.quarkus.modular.spi.items;

import io.quarkus.builder.item.MultiBuildItem;
import io.smallrye.common.constraint.Assert;

/**
 * Indicates that the named module must be a part of the boot module path.
 */
public final class BootModulePathBuildItem extends MultiBuildItem {
    private final String moduleName;

    /**
     * Construct a new instance.
     *
     * @param moduleName the module name for the boot path (must not be {@code null})
     */
    public BootModulePathBuildItem(final String moduleName) {
        this.moduleName = Assert.checkNotNullParam("moduleName", moduleName);
    }

    /**
     * {@return the module name}
     */
    public String moduleName() {
        return moduleName;
    }
}
