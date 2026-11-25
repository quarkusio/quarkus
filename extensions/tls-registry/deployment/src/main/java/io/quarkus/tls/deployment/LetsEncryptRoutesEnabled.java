package io.quarkus.tls.deployment;

import java.util.function.BooleanSupplier;

/**
 * Enabled when the application serves the Let's Encrypt challenge: for the Quarkus CLI flow, or for the client of
 * the application.
 */
public class LetsEncryptRoutesEnabled implements BooleanSupplier {

    private final LetsEncryptBuildTimeConfig config;

    LetsEncryptRoutesEnabled(LetsEncryptBuildTimeConfig config) {
        this.config = config;
    }

    @Override
    public boolean getAsBoolean() {
        return config.enabled() || config.client().enabled();
    }
}
