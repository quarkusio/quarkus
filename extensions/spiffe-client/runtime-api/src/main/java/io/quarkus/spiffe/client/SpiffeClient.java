package io.quarkus.spiffe.client;

import java.util.Set;

import io.smallrye.common.annotation.Experimental;
import io.smallrye.mutiny.Uni;

/**
 * Client for the SPIFFE Workload API.
 */
@Experimental("This API is currently experimental and might get changed")
public interface SpiffeClient {

    /**
     * Returns a SPIFFE JSON Web Token (JWT-SVID) from the Workload API for configured default audiences.
     *
     * @return a {@link Uni} that emits a single {@link WorkloadJsonWebToken}, never {@code null}; fails with
     *         {@link SpiffeAuthorizationException} when the workload is not authorized for any
     *         identity, or {@link SpiffeConnectionException} when the SPIRE Agent is unreachable
     * @throws IllegalStateException if no default audiences are configured via
     *         {@code quarkus.spiffe-client.audiences}
     */
    Uni<WorkloadJsonWebToken> getWorkloadJsonWebToken();

    /**
     * Returns a SPIFFE JSON Web Token (JWT-SVID) from the Workload API for a single audience.
     *
     * @param audience the audience value; must not be null, blank, or contain spaces
     * @return a {@link Uni} that emits a single {@link WorkloadJsonWebToken}, never {@code null}; fails with
     *         {@link SpiffeAuthorizationException} when the workload is not authorized for any
     *         identity, or {@link SpiffeConnectionException} when the SPIRE Agent is unreachable
     * @throws IllegalArgumentException if the audience is null, blank, or contains spaces
     */
    Uni<WorkloadJsonWebToken> getWorkloadJsonWebToken(String audience);

    /**
     * Returns a SPIFFE JSON Web Token (JWT-SVID) from the Workload API for the given audiences.
     *
     * @param audiences audience values; must not be null or empty, and each value must not be null, blank,
     *        or contain spaces
     * @return a {@link Uni} that emits a single {@link WorkloadJsonWebToken}, never {@code null}; fails with
     *         {@link SpiffeAuthorizationException} when the workload is not authorized for any
     *         identity, or {@link SpiffeConnectionException} when the SPIRE Agent is unreachable
     * @throws IllegalArgumentException if audiences is null, empty, or contains an invalid audience value
     */
    Uni<WorkloadJsonWebToken> getWorkloadJsonWebToken(Set<String> audiences);

}
