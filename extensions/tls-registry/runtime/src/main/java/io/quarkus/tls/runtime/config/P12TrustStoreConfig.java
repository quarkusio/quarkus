package io.quarkus.tls.runtime.config;

import java.nio.file.Path;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;

@ConfigGroup
public interface P12TrustStoreConfig {

    /**
     * Path to the trust store file (P12 / PFX format).
     */
    Path path();

    /**
     * Password of the trust store.
     * If not set, the password is retrieved from the credential provider, if configured.
     * Otherwise, the trust store is loaded without a password.
     */
    Optional<String> password();

    /**
     * Alias of the trust store.
     */
    Optional<String> alias();

    /**
     * Provider of the trust store.
     */
    Optional<String> provider();
}
