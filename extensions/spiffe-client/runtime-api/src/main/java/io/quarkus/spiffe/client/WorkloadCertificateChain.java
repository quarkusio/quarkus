package io.quarkus.spiffe.client;

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
     * Returns the workload X.509 certificate chain with the leaf certificate first. Never null or empty.
     */
    List<X509Certificate> certificateChain();

    /**
     * Returns the PKCS#8 private key associated with the leaf certificate of the workload certificate chain
     * returned in {@link #certificateChain()}. Never null.
     */
    PrivateKey privateKey();

    /**
     * Returns the workload certificate chain in PEM format with the leaf certificate first. Never null or empty.
     */
    List<String> certificateChainPem();

    /**
     * Returns the private key associated with the leaf certificate in PEM format. Never null.
     */
    String privateKeyPem();

}
