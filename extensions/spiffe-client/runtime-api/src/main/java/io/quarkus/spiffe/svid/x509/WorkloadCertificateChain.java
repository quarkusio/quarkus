package io.quarkus.spiffe.svid.x509;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.List;

import io.smallrye.common.annotation.Experimental;

/**
 * X.509-SVID workload certificate chain and private key.
 */
@Experimental("This API is currently experimental and might get changed")
public interface WorkloadCertificateChain {

    /**
     * Returns the X.509 workload certificate chain with the leaf certificate first. Never null or empty.
     */
    List<X509Certificate> chain();

    /**
     * Returns the private key associated with the leaf certificate of the X.509 workload certificate chain
     * returned in {@link #chain()}. Never null.
     */
    PrivateKey privateKey();

    /**
     * Returns the X.509 workload certificate chain in PEM format with the leaf certificate first. Never null or empty.
     */
    List<String> chainPem();

    /**
     * Returns the private key in PEM format associated with the leaf certificate of the X.509 workload certificate
     * chain returned in {@link #chainPem()}. Never null.
     */
    String privateKeyPem();

}
