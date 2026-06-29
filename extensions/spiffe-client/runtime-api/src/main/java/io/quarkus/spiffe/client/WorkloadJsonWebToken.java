package io.quarkus.spiffe.client;

import java.time.Instant;
import java.util.Set;

import io.smallrye.common.annotation.Experimental;

/**
 * A SPIFFE JSON Web Token (JWT-SVID) returned by the SPIFFE Workload API.
 */
@Experimental("This API is currently experimental and might get changed")
public interface WorkloadJsonWebToken {

    /**
     * Returns the serialized JWT token in JWS Compact Serialization format. Never null or empty.
     */
    String token();

    /**
     * Returns the subject ({@code sub} claim) of this JWT-SVID, which is the SPIFFE ID of the workload identity,
     * for example {@code spiffe://example.org/myservice}. Never null or empty.
     */
    String subject();

    /**
     * Returns the audience values from the JWT {@code aud} claim. Never null or empty.
     */
    Set<String> audience();

    /**
     * Returns the expiration time from the JWT {@code exp} claim. Never null.
     */
    Instant expiry();

}
