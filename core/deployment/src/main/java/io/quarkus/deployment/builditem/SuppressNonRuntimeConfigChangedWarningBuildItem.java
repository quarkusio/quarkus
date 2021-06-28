package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Allows extensions to suppress the runtime warning that Quarkus emits on startup when a non-runtime configuration
 * options is different at runtime than build time.
 * An example usage of this is when a user provides some test value in {@code application.properties}
 * for a build-time only property and only provides the actual value on the command line when building Quarkus.
 * In such a case we don't want the value set at build time to be revealed at runtime as it could be sensitive.
 */
public final class SuppressNonRuntimeConfigChangedWarningBuildItem extends MultiBuildItem {

    private final String configKey;

    public SuppressNonRuntimeConfigChangedWarningBuildItem(String configKey) {
        this.configKey = configKey;
    }

    public String getConfigKey() {
        return configKey;
    }
}
