package io.quarkus.tls;

import java.security.KeyStore;
import java.util.Optional;

import javax.net.ssl.SSLContext;

import io.vertx.core.net.KeyCertOptions;
import io.vertx.core.net.SSLOptions;
import io.vertx.core.net.TrustOptions;

/**
 * A base implementation of the transport layer security configuration interface.
 */
public abstract class BaseTlsConfiguration implements TlsConfiguration {

    /**
     * Returns the key store.
     *
     * @return the key store if configured.
     */
    public KeyStore getKeyStore() {
        return null;
    }

    /**
     * Returns the key store options.
     *
     * @return the key store options if configured.
     */
    public KeyCertOptions getKeyStoreOptions() {
        return null;
    }

    /**
     * Returns the trust store.
     *
     * @return the trust store if configured.
     */
    public KeyStore getTrustStore() {
        return null;
    }

    /**
     * Returns the trust store options.
     *
     * @return the trust store options if configured.
     */
    public TrustOptions getTrustStoreOptions() {
        return null;
    }

    /**
     * Returns the (Vert.x) SSL options.
     *
     * @return the {@link SSLOptions}, {@code null} if not configured.
     */
    public SSLOptions getSSLOptions() {
        return null;
    }

    /**
     * Creates and returns the SSL Context.
     *
     * @return the {@link SSLContext}, {@code null} if not configured.
     */
    public SSLContext createSSLContext() throws Exception {
        return null;
    }

    /**
     * Returns the hostname verification algorithm for this configuration.
     * {@code "NONE"} means no hostname verification.
     *
     * @return the hostname verification algorithm.
     */
    public Optional<String> getHostnameVerificationAlgorithm() {
        return Optional.empty();
    }

    /**
     * Returns whether the key store is configured to use SNI.
     * When SNI is used, the client indicate the server name during the TLS handshake, allowing the server to select the
     * right certificate.
     *
     * @return {@code true} if the key store is configured to use SNI, {@code false} otherwise.
     */
    public boolean usesSni() {
        return false;
    }

    /**
     * Reloads the configuration.
     * It usually means reloading the key store and trust store, especially when they are files.
     *
     * @return {@code true} if the configuration has been reloaded, {@code false} otherwise.
     */
    public boolean reload() {
        return false;
    }

    @Override
    public boolean isTrustAll() {
        return false;
    }
}
