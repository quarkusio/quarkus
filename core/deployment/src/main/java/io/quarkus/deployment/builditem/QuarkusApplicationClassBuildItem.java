package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.runtime.QuarkusApplication;

/**
 * Holds the name of the class implementing {@link io.quarkus.runtime.QuarkusApplication}.
 * <p>
 * This allows other build steps to identify the main application class
 * It stores the fully qualified class name as a {@code String}.
 */
public final class QuarkusApplicationClassBuildItem extends SimpleBuildItem {
    private final String className;

    public QuarkusApplicationClassBuildItem(Class<? extends QuarkusApplication> quarkusApplicationClass) {
        this.className = quarkusApplicationClass.getName();
    }

    public QuarkusApplicationClassBuildItem(String quarkusApplicationClassName) {
        this.className = quarkusApplicationClassName;
    }

    public String getClassName() {
        return className;
    }
}
