package io.quarkus.tls;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

/**
 * Configuration contract for a key store with an arbitrary type.
 * <p>
 * This interface is implemented by the runtime configuration class and is used by {@link KeyStoreFactory}
 * so that factory implementations can be defined without depending on the runtime configuration module.
 */
public interface OtherKeyStoreConfiguration {

    /**
     * The key store type, passed to {@code java.security.KeyStore.getInstance(type)}.
     * <p>
     * This value also serves as the lookup key for a {@code KeyStoreFactory} CDI bean via {@code @Identifier}.
     */
    String type();

    /**
     * The Java security provider name, passed to {@code java.security.KeyStore.getInstance(type, provider)}.
     */
    Optional<String> provider();

    /**
     * Path to the key store file.
     */
    Optional<Path> path();

    /**
     * Password of the key store.
     */
    Optional<String> password();

    /**
     * Alias of the private key and certificate in the key store.
     */
    Optional<String> alias();

    /**
     * Password of the alias in the key store.
     */
    Optional<String> aliasPassword();

    /**
     * Arbitrary parameters available to a {@code KeyStoreFactory} CDI bean via {@code config.params()}.
     */
    Map<String, String> params();
}
