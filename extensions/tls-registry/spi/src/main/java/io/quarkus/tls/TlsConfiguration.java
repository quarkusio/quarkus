package io.quarkus.tls;

import java.security.KeyStore;
import java.util.Optional;

import javax.net.ssl.SSLContext;

import io.vertx.core.net.ClientSSLOptions;
import io.vertx.core.net.KeyCertOptions;
import io.vertx.core.net.SSLEngineOptions;
import io.vertx.core.net.ServerSSLOptions;
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
     * Returns the server-side SSL options.
     *
     * @return the {@link ServerSSLOptions}, {@code null} if not configured.
     */
    ServerSSLOptions getServerSSLOptions();

    /**
     * Returns the client-side SSL options.
     *
     * @return the {@link ClientSSLOptions}, {@code null} if not configured.
     */
    ClientSSLOptions getClientSSLOptions();

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
     * Reloads the configuration from its source (e.g., file system).
     *
     * @return {@code true} if the reload succeeded (whether or not the content changed),
     *         {@code false} if the reload failed or the configuration is not reloadable.
     */
    boolean reload();

    /**
     * Returns a value identifying the key store, trust store and CRL content currently loaded.
     * <p>
     * The value changes when, and only when, a {@link #reload()} loads different content. A component applying
     * this configuration - an HTTP server updating its SSL options, a client rebuilding its connections - can
     * remember the value it last applied and compare it with the current one to know whether it has to apply the
     * configuration again. Each component keeps its own value, so a reload triggered by one of them does not hide
     * the change from the others.
     * <p>
     * A negative value means the implementation cannot tell whether the content changed; callers must then treat
     * every successful reload as a change.
     *
     * @return the version of the currently loaded content, or a negative value when it cannot be determined.
     */
    default long getContentVersion() {
        return -1;
    }

    /**
     * Returns the SSL engine options to use, if explicitly configured.
     * When empty, Vert.x selects the engine automatically based on the PQC enforcement policy.
     *
     * @return the {@link SSLEngineOptions} if configured, or empty.
     */
    default Optional<SSLEngineOptions> getSslEngineOptions() {
        return Optional.empty();
    }

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
