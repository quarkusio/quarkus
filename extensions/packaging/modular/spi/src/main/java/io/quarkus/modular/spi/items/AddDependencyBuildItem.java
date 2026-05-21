package io.quarkus.modular.spi.items;

import io.quarkus.builder.item.MultiBuildItem;
import io.smallrye.common.constraint.Assert;
import io.smallrye.modules.desc.Dependency;

/**
 * A build item which adds an extra dependency between modules.
 */
public final class AddDependencyBuildItem extends MultiBuildItem {
    private final String module;
    private final String targetModule;
    private final Dependency.Modifier.Set modifiers;

    /**
     * Construct a new instance.
     *
     * @param module the dependent module name (must not be {@code null})
     * @param targetModule the dependency module name (must not be {@code null})
     * @param modifiers the dependency modifiers (must not be {@code null})
     */
    public AddDependencyBuildItem(final String module, final String targetModule,
            final Dependency.Modifier.Set modifiers) {
        this.module = Assert.checkNotNullParam("module", module);
        this.targetModule = Assert.checkNotNullParam("targetModule", targetModule);
        this.modifiers = Assert.checkNotNullParam("modifiers", modifiers);
    }

    /**
     * {@return the dependent module name (not {@code null})}
     */
    public String module() {
        return module;
    }

    /**
     * {@return the dependency module name (not {@code null})}
     */
    public String targetModule() {
        return targetModule;
    }

    /**
     * {@return the dependency modifiers to apply (not {@code null})}
     */
    public Dependency.Modifier.Set modifiers() {
        return modifiers;
    }
}
