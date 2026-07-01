package io.quarkus.spiffe.client.api;

import java.time.Instant;
import java.util.Set;

/**
 * A parsed JWT-SVID (SPIFFE Verifiable Identity Document) returned by the SPIFFE Workload API.
 */
public interface JwtSvid {

    /**
     * Returns the serialized JWT token in JWS Compact Serialization format. Never null or blank.
     */
    String token();

    /**
     * Returns the SPIFFE ID of the workload identity, for example
     * {@code spiffe://example.org/myservice}. Never null or blank.
     */
    String spiffeId();

    /**
     * Returns the audience values from the JWT {@code aud} claim. Never null or empty.
     */
    Set<String> audience();

    /**
     * Returns the expiration time from the JWT {@code exp} claim. Never null.
     */
    Instant expiry();

}
