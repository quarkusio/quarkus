package io.quarkus.deployment.builditem;

import java.util.Objects;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * This will generate the equivalent of "--add-opens [module-name]=ALL-UNNAMED" for
 * all runners of the generated application.
 * It's currently only possible to open a module to ALL-UNNAMED; this restriction is dictated
 * by the limitations of the specification of the Jar's manifest format.
 */
public final class ModuleOpenBuildItem extends MultiBuildItem {
    private final String moduleName;

    public ModuleOpenBuildItem(String moduleName) {
        this.moduleName = Objects.requireNonNull(moduleName);
    }

    public String moduleName() {
        return moduleName;
    }
}
