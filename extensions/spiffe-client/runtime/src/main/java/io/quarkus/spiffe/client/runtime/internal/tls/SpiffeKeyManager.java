package io.quarkus.spiffe.client.runtime.internal.tls;

import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedKeyManager;

import org.jboss.logging.Logger;

import io.quarkus.spiffe.svid.x509.WorkloadCertificateDocument;
import io.vertx.core.Vertx;
import io.vertx.core.net.KeyCertOptions;

final class SpiffeKeyManager extends X509ExtendedKeyManager implements KeyCertOptions {

    private static final Logger LOG = Logger.getLogger(SpiffeKeyManager.class);
    private static final String ALIAS_PREFIX = "spiffe-";

    private final X509SvidCachingClient x509SvidCachingClient;
    private final String tlsConfigurationName;
    // the JSSE asks for the alias first and for the key and the chain afterwards; the alias names the document, so a
    // rotation in between never pairs a new key with an old chain
    private final AtomicReference<Chosen> chosen = new AtomicReference<>(new Chosen(null, null));

    SpiffeKeyManager(X509SvidCachingClient x509SvidCachingClient, String tlsConfigurationName) {
        this.x509SvidCachingClient = x509SvidCachingClient;
        this.tlsConfigurationName = tlsConfigurationName;
    }

    @Override
    public String[] getClientAliases(String keyType, Principal[] issuers) {
        return aliases(keyType);
    }

    @Override
    public String chooseClientAlias(String[] keyTypes, Principal[] issuers, Socket socket) {
        return chooseAlias(keyTypes);
    }

    @Override
    public String chooseEngineClientAlias(String[] keyTypes, Principal[] issuers, SSLEngine engine) {
        return chooseAlias(keyTypes);
    }

    @Override
    public String[] getServerAliases(String keyType, Principal[] issuers) {
        return aliases(keyType);
    }

    @Override
    public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
        return chooseAlias(keyType);
    }

    @Override
    public String chooseEngineServerAlias(String keyType, Principal[] issuers, SSLEngine engine) {
        return chooseAlias(keyType);
    }

    @Override
    public X509Certificate[] getCertificateChain(String alias) {
        WorkloadCertificateDocument document = document(alias);
        return document == null ? null : document.certificateChain().chain().toArray(new X509Certificate[0]);
    }

    @Override
    public PrivateKey getPrivateKey(String alias) {
        WorkloadCertificateDocument document = document(alias);
        return document == null ? null : document.certificateChain().privateKey();
    }

    @Override
    public KeyCertOptions copy() {
        // Vert.x caches SSL contexts by options equality, the same instance keeps the cache stable
        return this;
    }

    @Override
    public KeyManagerFactory getKeyManagerFactory(Vertx vertx) throws Exception {
        return KeyCertOptions.wrap(this).getKeyManagerFactory(vertx);
    }

    @Override
    public Function<String, KeyManagerFactory> keyManagerFactoryMapper(Vertx vertx) {
        // no SNI, Vert.x falls back to getKeyManagerFactory
        return serverName -> null;
    }

    @Override
    public String toString() {
        return "SpiffeKeyManager[" + tlsConfigurationName + "]";
    }

    private String[] aliases(String keyType) {
        String alias = chooseAlias(keyType);
        return alias == null ? null : new String[] { alias };
    }

    private String chooseAlias(String... keyTypes) {
        WorkloadCertificateDocument document = x509SvidCachingClient.getCurrentWorkloadCertificate();
        if (document == null) {
            // the caching client already warned about the missing, expired or revoked SVID
            LOG.debugf("No X.509-SVID is available, the TLS configuration '%s' cannot present a certificate",
                    tlsConfigurationName);
            return null;
        }
        String keyAlgorithm = document.certificateChain().privateKey().getAlgorithm();
        if (keyTypes == null || !Arrays.asList(keyTypes).contains(keyAlgorithm)) {
            LOG.debugf("The X.509-SVID key algorithm '%s' is not among the key types %s requested by the peer of the "
                    + "TLS configuration '%s'", keyAlgorithm, keyTypes == null ? null : List.of(keyTypes),
                    tlsConfigurationName);
            return null;
        }
        chosen.updateAndGet(c -> c.last == document ? c : new Chosen(document, c.last));
        return alias(document);
    }

    private WorkloadCertificateDocument document(String alias) {
        if (alias == null) {
            return null;
        }
        Chosen chosen = this.chosen.get();
        if (chosen.last != null && alias.equals(alias(chosen.last))) {
            return chosen.last;
        }
        if (chosen.previous != null && alias.equals(alias(chosen.previous))) {
            return chosen.previous;
        }
        return null;
    }

    private static String alias(WorkloadCertificateDocument document) {
        // a fingerprint of the leaf identifies the document even when a CA reuses serial numbers
        X509Certificate leaf = document.certificateChain().chain().getFirst();
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(leaf.getEncoded());
            return ALIAS_PREFIX + HexFormat.of().formatHex(digest, 0, 8);
        } catch (CertificateEncodingException | NoSuchAlgorithmException e) {
            throw new IllegalStateException("Unable to compute the fingerprint of the X.509-SVID", e);
        }
    }

    // the document handed out last and the one before it, for a handshake that straddles a rotation
    private record Chosen(WorkloadCertificateDocument last, WorkloadCertificateDocument previous) {
    }
}
