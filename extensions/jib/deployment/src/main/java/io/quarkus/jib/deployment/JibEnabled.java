package io.quarkus.jib.deployment;

import java.util.function.BooleanSupplier;

public class JibEnabled implements BooleanSupplier {

    private final JibConfig jibConfig;

    JibEnabled(JibConfig jibConfig) {
        this.jibConfig = jibConfig;
    }

    @Override
    public boolean getAsBoolean() {
        return jibConfig.enabled;
    }
}
