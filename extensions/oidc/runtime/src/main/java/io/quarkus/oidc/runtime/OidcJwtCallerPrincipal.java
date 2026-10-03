package io.quarkus.oidc.runtime;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import org.eclipse.microprofile.jwt.Claims;
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

    /**
     * Returns the {@code groups} claim. MicroProfile JWT requires an array, but some providers issue a single
     * space-separated string; that form is split here, the same way SmallRye JWT does for its own principals.
     */
    @Override
    public Set<String> getGroups() {
        Object groups = claims.getClaimValue(Claims.groups.name());
        if (groups instanceof String groupsString) {
            Set<String> result = new LinkedHashSet<>();
            for (String group : groupsString.split(" ")) {
                if (!group.isBlank()) {
                    result.add(group);
                }
            }
            return result;
        }
        return super.getGroups();
    }
}
