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
     * If not set, the password must be retrieved from the credential provider.
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
