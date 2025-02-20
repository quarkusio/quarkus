package io.quarkus.tls.deployment;

import java.util.function.BooleanSupplier;

public class LetsEncryptEnabled implements BooleanSupplier {

    private final LetsEncryptBuildTimeConfig config;

    LetsEncryptEnabled(LetsEncryptBuildTimeConfig config) {
        this.config = config;
    }

    @Override
    public boolean getAsBoolean() {
        return config.enabled();
    }

}
