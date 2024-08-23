
package io.quarkus.info.deployment;

import java.util.function.BooleanSupplier;

public class OsInInfoEndpointEnabled implements BooleanSupplier {

    private final InfoBuildTimeConfig config;

    OsInInfoEndpointEnabled(InfoBuildTimeConfig config) {
        this.config = config;
    }

    @Override
    public boolean getAsBoolean() {
        return config.enabled() && config.os().enabled();
    }
}
