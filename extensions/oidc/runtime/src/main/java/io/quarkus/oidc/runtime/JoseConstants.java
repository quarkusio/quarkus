package io.quarkus.oidc.runtime;

/**
 * JOSE header parameter names and JWK key types.
 */
final class JoseConstants {

    // JOSE header parameter names

    static final String ALGORITHM = "alg";
    static final String ENCRYPTION_ALGORITHM = "enc";
    static final String KEY_ID = "kid";
    static final String TYPE = "typ";
    static final String CONTENT_TYPE = "cty";
    static final String JWK = "jwk";
    static final String X_509_CERT_CHAIN = "x5c";
    static final String X_509_CERT_SHA_1_THUMBPRINT = "x5t";
    static final String X_509_CERT_SHA_256_THUMBPRINT = "x5t#S256";

    // JWK key types

    static final String RSA_KEY_TYPE = "RSA";
    static final String ELLIPTIC_CURVE_KEY_TYPE = "EC";
    // This key type is used when EdDSA algorithm is used
    static final String OCTET_KEY_PAIR_KEY_TYPE = "OKP";

    // JWK key uses

    static final String SIGNATURE_USE = "sig";
    static final String ENCRYPTION_USE = "enc";

    private JoseConstants() {
    }
}
