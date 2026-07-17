package io.quarkus.spiffe.client;

import java.security.cert.X509Certificate;
import java.util.List;

import io.smallrye.common.annotation.Experimental;
import io.vertx.core.net.TrustOptions;

/**
 * Trust material from an X.509-SVID: the CA certificates for the workload's trust domain.
 */
@Experimental("This API is currently experimental and might get changed")
public interface TrustMaterial {

    /**
     * Returns the CA certificates for the workload's own trust domain. Never null or empty.
     */
    List<X509Certificate> trustBundle();

    /**
     * Returns the trust bundle CA certificates in PEM format. Never null or empty.
     */
    List<String> trustBundlePem();

    /**
     * Returns Vert.x trust options built from the trust bundle with SPIFFE X.509-SVID leaf validation.
     */
    TrustOptions asVertxTrustOptions();

}
