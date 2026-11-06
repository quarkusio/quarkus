package io.quarkus.tls.runtime.config;

import java.nio.file.Path;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;

@ConfigGroup
public interface JKSKeyStoreConfig {

    /**
     * Path to the keystore file (JKS format).
     * <p>
     * Either this property or {@code value} must be set.
     */
    Optional<Path> path();

    /**
     * Content of the keystore (JKS format), encoded in base64.
     * Use it to pass the keystore inline (for example, from an environment variable) instead of reading it from a file.
     * <p>
     * Either this property or {@code path} must be set.
     */
    Optional<String> value();

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
     * When not set, the password may be retrieved from the credential provider.
     */
    Optional<String> aliasPassword();

    /**
     * Provider of the key store.
     */
    Optional<String> provider();

}
