package io.quarkus.tls.runtime.config;

import java.nio.file.Path;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;

@ConfigGroup
public interface P12KeyStoreConfig {

    /**
     * Path to the key store file (P12 / PFX format).
     */
    Path path();

    /**
     * Password of the key store.
     * When not set, the password must be retrieved from the credential provider.
     */
    Optional<String> password();

    /**
     * Alias of the private key and certificate in the key store.
     */
    Optional<String> alias();

    /**
     * Password of the alias in the key store.
     * If not set, the password will be retrieved from the credential provider.
     */
    Optional<String> aliasPassword();

    /**
     * Provider of the key store.
     */
    Optional<String> provider();
}
