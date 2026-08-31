package io.quarkus.spiffe.svid.x509;

import io.quarkus.spiffe.client.SpiffeAuthorizationException;
import io.quarkus.spiffe.client.SpiffeConnectionException;
import io.smallrye.common.annotation.Experimental;
import io.smallrye.mutiny.Uni;

/**
 * Client for the X.509-SVID profile of the SPIFFE Workload API.
 */
@Experimental("This API is currently experimental and might get changed")
public interface X509SvidClient {

    /**
     * Returns X.509 workload certificate document (X.509-SVID) which includes a client certificate chain and private key,
     * as well as a server trust bundle.
     *
     * @return a {@link Uni} that emits a single {@link WorkloadCertificateDocument}, never {@code null}; fails with
     *         {@link SpiffeAuthorizationException} when the workload is not authorized for any
     *         identity, or {@link SpiffeConnectionException} when the SPIRE Agent is unreachable
     */
    Uni<WorkloadCertificateDocument> getWorkloadCertificate();

}
