package io.quarkus.oidc.token.propagation.common.runtime;

public final class TokenPropagationConstants {

    TokenPropagationConstants() {
    }

    /**
     * System property key that is resolved to true if OIDC auth mechanism should put
     * `TokenCredential` into Vert.x duplicated context.
     */
    public static final String OIDC_PROPAGATE_TOKEN_CREDENTIAL = "io.quarkus.oidc.runtime." +
            "AbstractOidcAuthenticationMechanism.PROPAGATE_TOKEN_CREDENTIAL_WITH_DUPLICATED_CTX";
    /**
     * System property key that is resolved to true if JWT auth mechanism should put
     * `TokenCredential` into Vert.x duplicated context.
     */
    public static final String JWT_PROPAGATE_TOKEN_CREDENTIAL = "io.quarkus.smallrye.jwt.runtime." +
            "auth.JWTAuthMechanism.PROPAGATE_TOKEN_CREDENTIAL_WITH_DUPLICATED_CTX";

}
