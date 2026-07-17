package io.quarkus.spiffe.client;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.List;

import io.smallrye.common.annotation.Experimental;
import io.vertx.core.net.KeyCertOptions;

/**
 * Key material from an X.509-SVID: the workload certificate chain and private key.
 */
@Experimental("This API is currently experimental and might get changed")
public interface KeyMaterial {

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
     * Returns the certificate chain in PEM format with the leaf certificate first. Never null or empty.
     */
    List<String> certificateChainPem();

    /**
     * Returns the PKCS#8 private key in PEM format. Never null or empty.
     */
    String privateKeyPem();

    /**
     * Returns Vert.x PEM key-cert options built from the certificate chain and private key.
     */
    KeyCertOptions asVertxKeyCertOptions();

}
