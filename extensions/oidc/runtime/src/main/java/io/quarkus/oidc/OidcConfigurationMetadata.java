package io.quarkus.oidc;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class OidcConfigurationMetadata {
    private static final String ISSUER = "issuer";
    private static final String TOKEN_ENDPOINT = "token_endpoint";
    private static final String INTROSPECTION_ENDPOINT = "introspection_endpoint";
    private static final String AUTHORIZATION_ENDPOINT = "authorization_endpoint";
    private static final String JWKS_ENDPOINT = "jwks_uri";
    private static final String USERINFO_ENDPOINT = "userinfo_endpoint";
    private static final String END_SESSION_ENDPOINT = "end_session_endpoint";
    private static final String SCOPES_SUPPORTED = "scopes_supported";

    private final String tokenUri;
    private final String introspectionUri;
    private final String authorizationUri;
    private final String jsonWebKeySetUri;
    private final String userInfoUri;
    private final String endSessionUri;
    private final String issuer;
    private final JsonObject json;

    public OidcConfigurationMetadata(String tokenUri,
            String introspectionUri,
            String authorizationUri,
            String jsonWebKeySetUri,
            String userInfoUri,
            String endSessionUri,
            String issuer) {
        this.tokenUri = tokenUri;
        this.introspectionUri = introspectionUri;
        this.authorizationUri = authorizationUri;
        this.jsonWebKeySetUri = jsonWebKeySetUri;
        this.userInfoUri = userInfoUri;
        this.endSessionUri = endSessionUri;
        this.issuer = issuer;
        this.json = null;
    }

    public OidcConfigurationMetadata(JsonObject wellKnownConfig) {
        this.tokenUri = wellKnownConfig.getString(TOKEN_ENDPOINT);
        this.introspectionUri = wellKnownConfig.getString(INTROSPECTION_ENDPOINT);
        this.authorizationUri = wellKnownConfig.getString(AUTHORIZATION_ENDPOINT);
        this.jsonWebKeySetUri = wellKnownConfig.getString(JWKS_ENDPOINT);
        this.userInfoUri = wellKnownConfig.getString(USERINFO_ENDPOINT);
        this.endSessionUri = wellKnownConfig.getString(END_SESSION_ENDPOINT);
        this.issuer = wellKnownConfig.getString(ISSUER);
        this.json = wellKnownConfig;
    }

    public String getTokenUri() {
        return tokenUri;
    }

    public String getIntrospectionUri() {
        return introspectionUri;
    }

    public String getAuthorizationUri() {
        return authorizationUri;
    }

    public String getJsonWebKeySetUri() {
        return jsonWebKeySetUri;
    }

    public String getUserInfoUri() {
        return userInfoUri;
    }

    public String getEndSessionUri() {
        return endSessionUri;
    }

    public List<String> getSupportedScopes() {
        return getStringList(SCOPES_SUPPORTED);
    }

    public String getIssuer() {
        return issuer;
    }

    public String get(String propertyName) {
        return json == null ? null : json.getString(propertyName);
    }

    public List<String> getStringList(String propertyName) {
        JsonArray array = json == null ? null : json.getJsonArray(propertyName);
        if (array != null) {
            @SuppressWarnings("unchecked")
            List<String> values = array.getList();
            return Collections.unmodifiableList(values);
        } else {
            return null;
        }
    }

    public boolean contains(String propertyName) {
        return json == null ? false : json.containsKey(propertyName);
    }

    public Set<String> getPropertyNames() {
        return Collections.unmodifiableSet(json.fieldNames());
    }
}
