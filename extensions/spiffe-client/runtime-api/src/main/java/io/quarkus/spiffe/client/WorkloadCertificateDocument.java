package io.quarkus.spiffe.client;

import io.smallrye.common.annotation.Experimental;

/**
 * An X.509 workload certificate document (X.509-SVID) returned by the SPIFFE Workload API.
 */
@Experimental("This API is currently experimental and might get changed")
public interface WorkloadCertificateDocument {

    /**
     * Returns the subject (URI SAN) of this X.509-SVID, which is the SPIFFE ID of the workload identity,
     * for example {@code spiffe://example.org/myservice}. Never null or empty.
     */
    String subject();

    /**
     * Returns the key material: workload certificate chain and private key.
     */
    KeyMaterial keyMaterial();

    /**
     * Returns the trust material: CA certificates for the workload's trust domain.
     */
    TrustMaterial trustMaterial();

}
