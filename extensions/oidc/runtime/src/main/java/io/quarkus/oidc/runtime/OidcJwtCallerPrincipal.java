package io.quarkus.oidc.runtime;

import java.util.Optional;

import org.jose4j.jwt.JwtClaims;

import io.quarkus.security.credential.TokenCredential;
import io.smallrye.jwt.auth.principal.DefaultJWTCallerPrincipal;

/**
 * An implementation of JWTCallerPrincipal
 */
public class OidcJwtCallerPrincipal extends DefaultJWTCallerPrincipal {
    private final JwtClaims claims;
    private final String principalClaim;
    private final TokenCredential credential;

    public OidcJwtCallerPrincipal(final JwtClaims claims, TokenCredential credential) {
        this(claims, credential, null);
    }

    public OidcJwtCallerPrincipal(final JwtClaims claims, TokenCredential credential, String principalClaim) {
        super(claims);
        this.claims = claims;
        this.credential = credential;
        this.principalClaim = principalClaim;
    }

    public JwtClaims getClaims() {
        return claims;
    }

    public TokenCredential getCredential() {
        return credential;
    }

    @Override
    public String getName() {
        if (principalClaim != null) {
            Optional<String> claim = super.claim(principalClaim);
            return claim.orElse(null);
        } else {
            return super.getName();
        }
    }
}
