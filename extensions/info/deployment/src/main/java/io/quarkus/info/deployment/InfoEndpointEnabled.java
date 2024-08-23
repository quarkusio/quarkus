
package io.quarkus.info.deployment;

import java.util.function.BooleanSupplier;

public class InfoEndpointEnabled implements BooleanSupplier {

    private final InfoBuildTimeConfig config;

    InfoEndpointEnabled(InfoBuildTimeConfig config) {
        this.config = config;
    }

    @Override
    public boolean getAsBoolean() {
        return config.enabled();
    }
}
