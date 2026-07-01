package io.quarkus.spiffe.client.api;

import io.smallrye.mutiny.Uni;

/**
 * Client for the SPIFFE Workload API. Calls are not retried automatically; callers should
 * implement their own retry logic if needed.
 */
public interface SpiffeClient {

    /**
     * Fetches a JWT-SVID from the Workload API using the configured default audiences.
     *
     * @return a {@link Uni} that emits a single {@link JwtSvid}, never {@code null}; fails with
     *         {@link SpiffeAuthorizationException} when the workload is not authorized for any
     *         identity, or {@link SpiffeConnectionException} when the SPIRE Agent is unreachable
     * @throws IllegalStateException if no default audiences are configured via
     *         {@code quarkus.spiffe-client.audiences}
     */
    Uni<JwtSvid> fetchJwtSvid();

    /**
     * Fetches a JWT-SVID from the Workload API.
     * <p>
     * When the request specifies audiences, they are used exclusively. When the request has no audiences,
     * the configured default audiences ({@code quarkus.spiffe-client.audiences}) are used as a fallback.
     *
     * @param request the fetch parameters; never null
     * @return a {@link Uni} that emits a single {@link JwtSvid}, never {@code null}; fails with
     *         {@link SpiffeAuthorizationException} when the workload is not authorized for any
     *         identity, or {@link SpiffeConnectionException} when the SPIRE Agent is unreachable
     * @throws IllegalArgumentException if request is null or no audiences are available from either the request
     *         or the configuration
     */
    Uni<JwtSvid> fetchJwtSvid(JwtSvidRequest request);

}
