package io.quarkus.tls.runtime.internal;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Snapshot of TLS material, comparable by value to detect a certificate rotation.
 * <p>
 * Options built by a {@code KeyStoreProvider} or {@code KeyStoreFactory} may not embed the store content (path-based
 * or PKCS#11 options) and stay equal after a rotation, so the certificates of the stores are compared as well.
 * <p>
 * This interface is not part of the public API.
 */
public interface CertificateSnapshot {

    /**
     * @param keyStore the key store, may be {@code null}
     * @param trustStore the trust store, may be {@code null}
     * @param options the options to compare as well (Vert.x options, CRLs...), elements may be {@code null}
     * @return a value equal to the snapshot of the same content
     */
    static List<Object> of(KeyStore keyStore, KeyStore trustStore, Object... options) {
        Object[] content = Arrays.copyOf(options, options.length + 2);
        content[options.length] = certificates(keyStore);
        content[options.length + 1] = certificates(trustStore);
        return Arrays.asList(content);
    }

    /**
     * @return the certificate chains by alias, {@code null} if the store is {@code null}, or an object equal to no
     *         other if the store cannot be read
     */
    private static Object certificates(KeyStore store) {
        if (store == null) {
            return null;
        }
        try {
            // The whole chain, so a re-issued intermediate is detected even if the leaf is unchanged
            Map<String, List<Certificate>> certificates = new HashMap<>();
            for (String alias : Collections.list(store.aliases())) {
                Certificate[] chain = store.getCertificateChain(alias);
                certificates.put(alias,
                        chain != null ? Arrays.asList(chain) : Collections.singletonList(store.getCertificate(alias)));
            }
            return certificates;
        } catch (KeyStoreException e) {
            // Cannot tell, so report a change: an unneeded notification beats a missed rotation
            return new Object();
        }
    }
}
