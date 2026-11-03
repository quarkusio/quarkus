package io.quarkus.tls.deployment;

import java.util.function.BooleanSupplier;

/**
 * Enabled when the Let's Encrypt client of the application is enabled.
 */
public class LetsEncryptClientEnabled implements BooleanSupplier {

    private final LetsEncryptBuildTimeConfig config;

    LetsEncryptClientEnabled(LetsEncryptBuildTimeConfig config) {
        this.config = config;
    }

    @Override
    public boolean getAsBoolean() {
        return config.client().enabled();
    }
}
