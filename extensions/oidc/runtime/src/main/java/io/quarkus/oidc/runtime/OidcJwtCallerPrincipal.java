package io.quarkus.oidc.runtime;

import org.jose4j.jwt.JwtClaims;

import io.smallrye.jwt.auth.principal.DefaultJWTCallerPrincipal;

/**
 * An implementation of JWTCallerPrincipal that builds on the Elytron attributes
 */
public class OidcJwtCallerPrincipal extends DefaultJWTCallerPrincipal {
    private JwtClaims claims;

    public OidcJwtCallerPrincipal(final JwtClaims claims) {
        super(claims);
        this.claims = claims;
    }

    public JwtClaims getClaims() {
        return claims;
    }
}
