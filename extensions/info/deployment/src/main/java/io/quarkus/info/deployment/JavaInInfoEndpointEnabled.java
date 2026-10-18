
package io.quarkus.info.deployment;

import java.util.function.BooleanSupplier;

public class JavaInInfoEndpointEnabled implements BooleanSupplier {

    private final InfoBuildTimeConfig config;

    JavaInInfoEndpointEnabled(InfoBuildTimeConfig config) {
        this.config = config;
    }

    @Override
    public boolean getAsBoolean() {
        return config.enabled() && config.java().enabled();
    }
}
