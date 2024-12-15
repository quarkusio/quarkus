package io.quarkus.tls.runtime.keystores;

import java.io.UncheckedIOException;
import java.security.KeyStore;

import io.quarkus.tls.runtime.KeyStoreAndKeyCertOptions;
import io.quarkus.tls.runtime.TrustStoreAndTrustOptions;
import io.quarkus.tls.runtime.config.KeyStoreConfig;
import io.quarkus.tls.runtime.config.PemKeyCertConfig;
import io.quarkus.tls.runtime.config.TrustStoreConfig;
import io.vertx.core.Vertx;
import io.vertx.core.net.PemKeyCertOptions;

/**
 * A utility class to validate PEM key store and trust store configurations.
 */
public class PemKeyStores {

    private PemKeyStores() {
        // Avoid direct instantiation
    }

    public static KeyStoreAndKeyCertOptions verifyPEMKeyStore(KeyStoreConfig ksc, Vertx vertx, String name) {
        PemKeyCertConfig config = ksc.pem().orElseThrow();
        if (config.keyCerts().isEmpty()) {
            throw new IllegalStateException("No key/certificate pair configured for certificate '" + name + "'");
        }
        try {
            PemKeyCertOptions options = config.toOptions();
            return new KeyStoreAndKeyCertOptions(options.loadKeyStore(vertx), options);
        } catch (UncheckedIOException e) {
            throw new IllegalStateException("Invalid key/certificate pair configuration for certificate '" + name
                    + "' - cannot read the key/certificate files", e);
        } catch (Exception e) {
            throw new IllegalStateException("Invalid key/certificate pair configuration for certificate '" + name + "'", e);
        }
    }

    public static TrustStoreAndTrustOptions verifyPEMTrustStoreStore(TrustStoreConfig tsc, Vertx vertx, String name) {
        var config = tsc.pem().orElseThrow();
        if (config.certs().isEmpty() || config.certs().get().isEmpty()) {
            throw new IllegalStateException("No PEM certificates configured for the trust store of '" + name + "'");
        }
        try {
            var options = config.toOptions();
            KeyStore ks = options.loadKeyStore(vertx);
            if (tsc.certificateExpirationPolicy() == TrustStoreConfig.CertificateExpiryPolicy.IGNORE) {
                return new TrustStoreAndTrustOptions(ks, options);
            } else {
                var wrapped = new ExpiryTrustOptions(options, tsc.certificateExpirationPolicy());
                return new TrustStoreAndTrustOptions(ks, wrapped);
            }
        } catch (UncheckedIOException e) {
            throw new IllegalStateException("Invalid PEM trusted certificates configuration for certificate '" + name
                    + "' - cannot read the PEM certificate files", e);
        } catch (Exception e) {
            throw new IllegalStateException("Invalid PEM trusted certificates configuration for certificate '" + name + "'", e);
        }
    }
}
