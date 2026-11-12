package io.quarkus.modular.spi.items;

import io.quarkus.builder.item.MultiBuildItem;
import io.smallrye.common.constraint.Assert;

/**
 * Assigns a generated class to a module without requiring its producer to discover module names.
 * This affects modular packaging only; it does not change application class loading in dev mode.
 */
public final class GeneratedClassPlacementBuildItem extends MultiBuildItem {
    private final String generatedClass;
    private final String anchorClass;

    private GeneratedClassPlacementBuildItem(String generatedClass, String anchorClass) {
        this.generatedClass = Assert.checkNotEmptyParam("generatedClass", generatedClass).replace('/', '.');
        this.anchorClass = anchorClass;
    }

    /** Places the generated class in the module containing the anchor, which may itself be generated. */
    public static GeneratedClassPlacementBuildItem inModuleOf(String generatedClass, String anchorClass) {
        return new GeneratedClassPlacementBuildItem(generatedClass,
                Assert.checkNotEmptyParam("anchorClass", anchorClass).replace('/', '.'));
    }

    /** Places the generated class in the application module. */
    public static GeneratedClassPlacementBuildItem inApplication(String generatedClass) {
        return new GeneratedClassPlacementBuildItem(generatedClass, null);
    }

    public String generatedClass() {
        return generatedClass;
    }

    /** Returns the anchor's binary name, or {@code null} for the application module. */
    public String anchorClass() {
        return anchorClass;
    }
}
