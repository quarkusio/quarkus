
package io.quarkus.info.deployment;

import java.util.function.BooleanSupplier;

public class GitInInfoEndpointEnabled implements BooleanSupplier {

    private final InfoBuildTimeConfig config;

    GitInInfoEndpointEnabled(InfoBuildTimeConfig config) {
        this.config = config;
    }

    @Override
    public boolean getAsBoolean() {
        return config.enabled() && config.git().enabled();
    }
}
