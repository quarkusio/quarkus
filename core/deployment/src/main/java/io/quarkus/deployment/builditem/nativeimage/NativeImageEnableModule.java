package io.quarkus.deployment.builditem.nativeimage;

import java.util.Objects;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Register a JVM module to be enabled via GraalVM's {@code --add-modules}
 * argument.
 * Not all modules are enabled by default, and some libraries might
 * need to enlist some of the non-default modules to be activated.
 */
public final class NativeImageEnableModule extends MultiBuildItem {

    private final String moduleName;

    public NativeImageEnableModule(String moduleName) {
        this.moduleName = Objects.requireNonNull(moduleName);
    }

    public String getModuleName() {
        return moduleName;
    }
}
