package io.quarkus.tls.runtime;

import java.security.KeyStore;

import io.vertx.core.net.KeyCertOptions;

/**
 * A structure storing a key store and its associated Vert.x options.
 */
public final class KeyStoreAndKeyCertOptions {
    public final KeyStore keyStore;
    public final KeyCertOptions options;

    public KeyStoreAndKeyCertOptions(KeyStore keyStore, KeyCertOptions options) {
        this.keyStore = keyStore;
        this.options = options;
    }
}
