package io.quarkus.oidc;

/**
 * When a DPoP proof must include a nonce, register an implementation of this interface as a CDI bean
 * to provide and validate a nonce value.
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc9449#name-resource-server-provided-no">RFC 9449</a>
 */
public interface DPoPNonceProvider {

    /**
     * Provides a nonce that must be included in the DPoP proof as the "nonce" claim.
     *
     * @return resource server nonce
     */
    String getNonce();

    /**
     * Determines if a DPoP proof nonce is valid. Implementations must check that this nonce exists and has not expired.
     *
     * @param nonce DPoP proof nonce
     * @return true if the `nonce` is valid
     */
    boolean isValid(String nonce);

}
