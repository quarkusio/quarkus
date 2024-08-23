package io.quarkus.tls.runtime;

import java.security.KeyStore;

import io.vertx.core.net.TrustOptions;

/**
 * A structure storing a trust store and its associated Vert.x options.
 */
public final class TrustStoreAndTrustOptions {
    public final KeyStore trustStore;
    public final TrustOptions options;

    public TrustStoreAndTrustOptions(KeyStore keyStore, TrustOptions options) {
        this.trustStore = keyStore;
        this.options = options;

    }
}
