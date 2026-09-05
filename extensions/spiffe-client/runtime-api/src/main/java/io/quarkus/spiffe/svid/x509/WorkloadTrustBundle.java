package io.quarkus.spiffe.svid.x509;

import java.security.cert.X509Certificate;
import java.util.List;

import io.smallrye.common.annotation.Experimental;

/**
 * X.509-SVID CA certificates of the workload's trust domain.
 */
@Experimental("This API is currently experimental and might get changed")
public interface WorkloadTrustBundle {

    /**
     * Returns the X.509 CA certificates of the workload's trust domain.
     * The bundle can contain more than one certificate, for example, while the
     * trust domain CA is being rotated. Never null or empty.
     */
    List<X509Certificate> certificates();

    /**
     * Returns the X.509 CA certificates of the workload's trust domain in PEM format. Never null or empty.
     */
    List<String> certificatesPem();

}
