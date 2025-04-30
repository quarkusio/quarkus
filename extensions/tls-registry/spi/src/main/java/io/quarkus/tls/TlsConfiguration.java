package io.quarkus.tls;

import java.security.KeyStore;
import java.util.Optional;

import javax.net.ssl.SSLContext;

import io.vertx.core.net.KeyCertOptions;
import io.vertx.core.net.SSLOptions;
import io.vertx.core.net.TrustOptions;

/**
 * The transport layer security configuration.
 */
public interface TlsConfiguration {

    static Optional<TlsConfiguration> from(TlsConfigurationRegistry registry, Optional<String> name) {
        if (name.isPresent()) {
            Optional<TlsConfiguration> maybeConfiguration = registry.get(name.get());
            if (maybeConfiguration.isEmpty()) {
                throw new IllegalStateException("Unable to find the TLS configuration for name " + name.get() + ".");
            }
            return maybeConfiguration;
        }
        return Optional.empty();
    }

    /**
     * Returns the key store.
     *
     * @return the key store if configured.
     */
    KeyStore getKeyStore();

    /**
     * Returns the key store options.
     *
     * @return the key store options if configured.
     */
    KeyCertOptions getKeyStoreOptions();

    /**
     * Returns the trust store.
     *
     * @return the trust store if configured.
     */
    KeyStore getTrustStore();

    /**
     * Returns the trust store options.
     *
     * @return the trust store options if configured.
     */
    TrustOptions getTrustStoreOptions();

    /**
     * Returns the (Vert.x) SSL options.
     *
     * @return the {@link SSLOptions}, {@code null} if not configured.
     */
    SSLOptions getSSLOptions();

    /**
     * Creates and returns the SSL Context.
     *
     * @return the {@link SSLContext}, {@code null} if not configured.
     * @throws Exception if the SSL Context cannot be created.
     */
    SSLContext createSSLContext() throws Exception;

    /**
     * Returns whether the trust store is configured to trust all certificates.
     *
     * @return {@code true} if the trust store is configured to trust all certificates, {@code false} otherwise.
     */
    boolean isTrustAll();

    /**
     * Returns the hostname verification algorithm for this configuration.
     * {@code "NONE"} means no hostname verification.
     *
     * @return the hostname verification algorithm.
     */
    Optional<String> getHostnameVerificationAlgorithm();

    /**
     * Returns whether the key store is configured to use SNI.
     * When SNI is used, the client indicate the server name during the TLS handshake, allowing the server to select the
     * right certificate.
     *
     * @return {@code true} if the key store is configured to use SNI, {@code false} otherwise.
     */
    boolean usesSni();

    /**
     * Reloads the configuration.
     * It usually means reloading the key store and trust store, especially when they are files.
     *
     * @return {@code true} if the configuration has been reloaded, {@code false} otherwise.
     */
    boolean reload();

    /**
     * Returns the name which was associated with this configuration
     * <p>
     * Note: Although this was made default in order to not break deep integrations, it is strongly recommended that the method
     * be implemented.
     */
    default String getName() {
        return "unset";
    }

}
