package io.quarkus.tls.runtime.config;

import java.nio.file.Path;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;

@ConfigGroup
public interface JKSTrustStoreConfig {

    /**
     * Path to the trust store file (JKS format).
     */
    Path path();

    /**
     * Password of the trust store.
     * If not set, the password must be retrieved from the credential provider.
     */
    Optional<String> password();

    /**
     * Alias of the key in the trust store.
     */
    Optional<String> alias();

    /**
     * Provider of the trust store.
     */
    Optional<String> provider();

}
