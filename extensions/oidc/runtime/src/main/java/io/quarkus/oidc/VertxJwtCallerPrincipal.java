package io.quarkus.oidc;

import org.jose4j.jwt.JwtClaims;

import io.smallrye.jwt.auth.principal.DefaultJWTCallerPrincipal;

/**
 * An implementation of JWTCallerPrincipal that builds on the Elytron attributes
 */
public class VertxJwtCallerPrincipal extends DefaultJWTCallerPrincipal {
    private JwtClaims claims;

    public VertxJwtCallerPrincipal(final JwtClaims claims) {
        super(claims);
        this.claims = claims;
    }

    public JwtClaims getClaims() {
        return claims;
    }
}
