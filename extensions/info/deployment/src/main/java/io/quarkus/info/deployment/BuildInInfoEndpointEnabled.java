
package io.quarkus.info.deployment;

import java.util.function.BooleanSupplier;

public class BuildInInfoEndpointEnabled implements BooleanSupplier {

    private final InfoBuildTimeConfig config;

    BuildInInfoEndpointEnabled(InfoBuildTimeConfig config) {
        this.config = config;
    }

    @Override
    public boolean getAsBoolean() {
        return config.enabled() && config.build().enabled();
    }
}
