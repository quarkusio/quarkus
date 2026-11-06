package io.quarkus.tls.runtime.config;

import java.nio.file.Path;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;

@ConfigGroup
public interface P12TrustStoreConfig {

    /**
     * Path to the trust store file (P12 / PFX format).
     * <p>
     * Either this property or {@code value} must be set.
     */
    Optional<Path> path();

    /**
     * Content of the trust store (P12 / PFX format), encoded in base64.
     * Use it to pass the trust store inline (for example, from an environment variable) instead of reading it from a file.
     * <p>
     * Either this property or {@code path} must be set.
     */
    Optional<String> value();

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
