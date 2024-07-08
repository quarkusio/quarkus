
package io.quarkus.tekton.deployment;

import java.util.function.BooleanSupplier;

public class TektonEnabled implements BooleanSupplier {

    private final TektonConfig config;

    public TektonEnabled(TektonConfig config) {
        this.config = config;
    }

    @Override
    public boolean getAsBoolean() {
        return config.enabled;
    }
}
