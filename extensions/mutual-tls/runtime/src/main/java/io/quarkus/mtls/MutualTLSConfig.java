package io.quarkus.mtls;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

public class MutualTLSConfig {

    private List<X509Certificate> identityCertificateChain;
    private PrivateKey identityPrivateKey;
    private List<X509Certificate> trustedCertificates;
    private Instant expiresAt;

    public MutualTLSConfig(List<X509Certificate> identityCertificateChain, PrivateKey identityPrivateKey,
            List<X509Certificate> trustedCertificates, Instant expiresAt) {
        this.identityCertificateChain = Collections.unmodifiableList(identityCertificateChain);
        this.identityPrivateKey = identityPrivateKey;
        this.trustedCertificates = Collections.unmodifiableList(trustedCertificates);
        this.expiresAt = expiresAt;
    }

    public List<X509Certificate> getIdentityCertificateChain() {
        return identityCertificateChain;
    }

    public PrivateKey getIdentityPrivateKey() {
        return identityPrivateKey;
    }

    public List<X509Certificate> getTrustedCertificates() {
        return trustedCertificates;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
