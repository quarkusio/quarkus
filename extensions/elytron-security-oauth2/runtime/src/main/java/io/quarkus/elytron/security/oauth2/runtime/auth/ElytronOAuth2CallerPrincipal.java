package io.quarkus.elytron.security.oauth2.runtime.auth;

import java.security.Principal;
import java.util.Map;
import java.util.Optional;

/**
 * An implementation of ElytronOAuth2CallerPrincipal that builds on the Elytron attributes
 */
public class ElytronOAuth2CallerPrincipal implements Principal {
    private Map<String, Object> claims;
    private String customPrincipalName;

    public ElytronOAuth2CallerPrincipal(final String customPrincipalName, final Map<String, Object> claims) {
        this.claims = claims;
        this.customPrincipalName = customPrincipalName;
    }

    public ElytronOAuth2CallerPrincipal(final Map<String, Object> claims) {
        this("username", claims);
    }

    public Map<String, Object> getClaims() {
        return claims;
    }

    @Override
    public String getName() {
        return getClaimValueAsString(customPrincipalName).orElseGet(() -> getClaimValueAsString("client_id").orElse(null));
    }

    private Optional<String> getClaimValueAsString(String key) {
        if (getClaims().containsKey(key)) {
            return Optional.of((String) getClaims().get(key));
        }
        return Optional.empty();
    }
}
