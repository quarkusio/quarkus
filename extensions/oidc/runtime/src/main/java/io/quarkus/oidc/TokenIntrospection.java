package io.quarkus.oidc;

import java.util.HashSet;
import java.util.Set;

import jakarta.json.JsonObject;

import io.quarkus.oidc.common.runtime.AbstractJsonObject;
import io.quarkus.oidc.common.runtime.OidcConstants;

/**
 * Represents a token introspection result
 *
 */
public class TokenIntrospection extends AbstractJsonObject {

    public TokenIntrospection() {
    }

    public TokenIntrospection(String introspectionJson) {
        super(introspectionJson);
    }

    public TokenIntrospection(JsonObject json) {
        super(json);
    }

    public boolean isActive() {
        return getBoolean(OidcConstants.INTROSPECTION_TOKEN_ACTIVE);
    }

    public String getUsername() {
        return getString(OidcConstants.INTROSPECTION_TOKEN_USERNAME);
    }

    public String getSubject() {
        return getString(OidcConstants.INTROSPECTION_TOKEN_SUB);
    }

    public String getAudience() {
        return getString(OidcConstants.INTROSPECTION_TOKEN_AUD);
    }

    public String getIssuer() {
        return getString(OidcConstants.INTROSPECTION_TOKEN_ISS);
    }

    public Set<String> getScopes() {
        if (this.contains(OidcConstants.TOKEN_SCOPE)) {
            String[] scopesArray = getString(OidcConstants.TOKEN_SCOPE).split(" ");
            Set<String> scopes = new HashSet<>(scopesArray.length);
            for (String scope : scopesArray) {
                scopes.add(scope.trim());
            }
            return scopes;
        } else {
            return null;
        }

    }

    public String getClientId() {
        return getString(OidcConstants.INTROSPECTION_TOKEN_CLIENT_ID);
    }

    public String getIntrospectionString() {
        return getJsonString();
    }
}
