package io.quarkus.smallrye.jwt.runtime.auth;

import org.jose4j.jwt.JwtClaims;

import io.smallrye.jwt.auth.principal.DefaultJWTCallerPrincipal;

/**
 * An implementation of JWTCallerPrincipal that builds on the Elytron attributes
 */
public class QuarkusJwtCallerPrincipal extends DefaultJWTCallerPrincipal {
    private JwtClaims claims;
    private String customPrincipalName;

    public QuarkusJwtCallerPrincipal(final String customPrincipalName, final JwtClaims claims) {
        super(claims);
        this.claims = claims;
        this.customPrincipalName = customPrincipalName;
    }

    public JwtClaims getClaims() {
        return claims;
    }

    @Override
    public String getName() {
        return customPrincipalName != null ? customPrincipalName : super.getName();
    }

}
