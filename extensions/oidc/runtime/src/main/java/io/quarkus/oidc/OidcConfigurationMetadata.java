package io.quarkus.oidc;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class OidcConfigurationMetadata {
    public static final String ISSUER = "issuer";
    public static final String TOKEN_ENDPOINT = "token_endpoint";
    public static final String INTROSPECTION_ENDPOINT = "introspection_endpoint";
    public static final String AUTHORIZATION_ENDPOINT = "authorization_endpoint";
    public static final String JWKS_ENDPOINT = "jwks_uri";
    public static final String USERINFO_ENDPOINT = "userinfo_endpoint";
    public static final String END_SESSION_ENDPOINT = "end_session_endpoint";
    private static final String REGISTRATION_ENDPOINT = "registration_endpoint";
    private static final String REVOCATION_ENDPOINT = "revocation_endpoint";
    public static final String SCOPES_SUPPORTED = "scopes_supported";

    private final String discoveryUri;
    private final String tokenUri;
    private final String introspectionUri;
    private final String authorizationUri;
    private final String jsonWebKeySetUri;
    private final String userInfoUri;
    private final String endSessionUri;
    private final String registrationUri;
    private final String revocationUri;
    private final String issuer;
    private final JsonObject json;

    public OidcConfigurationMetadata(String tokenUri,
            String introspectionUri,
            String authorizationUri,
            String jsonWebKeySetUri,
            String userInfoUri,
            String endSessionUri,
            String registrationUri,
            String revocationUri,
            String issuer) {
        this.discoveryUri = null;
        this.tokenUri = tokenUri;
        this.introspectionUri = introspectionUri;
        this.authorizationUri = authorizationUri;
        this.jsonWebKeySetUri = jsonWebKeySetUri;
        this.userInfoUri = userInfoUri;
        this.endSessionUri = endSessionUri;
        this.registrationUri = registrationUri;
        this.revocationUri = revocationUri;
        this.issuer = issuer;
        this.json = null;
    }

    public OidcConfigurationMetadata(JsonObject wellKnownConfig) {
        this(wellKnownConfig, null, null);
    }

    public OidcConfigurationMetadata(JsonObject wellKnownConfig, OidcConfigurationMetadata localMetadataConfig,
            String discoveryUri) {
        this.discoveryUri = discoveryUri;
        this.tokenUri = getMetadataValue(wellKnownConfig, TOKEN_ENDPOINT,
                localMetadataConfig == null ? null : localMetadataConfig.tokenUri);
        this.introspectionUri = getMetadataValue(wellKnownConfig, INTROSPECTION_ENDPOINT,
                localMetadataConfig == null ? null : localMetadataConfig.introspectionUri);
        this.authorizationUri = getMetadataValue(wellKnownConfig, AUTHORIZATION_ENDPOINT,
                localMetadataConfig == null ? null : localMetadataConfig.authorizationUri);
        this.jsonWebKeySetUri = getMetadataValue(wellKnownConfig, JWKS_ENDPOINT,
                localMetadataConfig == null ? null : localMetadataConfig.jsonWebKeySetUri);
        this.userInfoUri = getMetadataValue(wellKnownConfig, USERINFO_ENDPOINT,
                localMetadataConfig == null ? null : localMetadataConfig.userInfoUri);
        this.endSessionUri = getMetadataValue(wellKnownConfig, END_SESSION_ENDPOINT,
                localMetadataConfig == null ? null : localMetadataConfig.endSessionUri);
        this.registrationUri = getMetadataValue(wellKnownConfig, REGISTRATION_ENDPOINT,
                localMetadataConfig == null ? null : localMetadataConfig.registrationUri);
        this.revocationUri = getMetadataValue(wellKnownConfig, REVOCATION_ENDPOINT,
                localMetadataConfig == null ? null : localMetadataConfig.revocationUri);
        this.issuer = getMetadataValue(wellKnownConfig, ISSUER,
                localMetadataConfig == null ? null : localMetadataConfig.issuer);
        this.json = wellKnownConfig;
    }

    private static String getMetadataValue(JsonObject wellKnownConfig, String propertyName, String localValue) {
        return localValue != null ? localValue : wellKnownConfig.getString(propertyName);
    }

    public String getDiscoveryUri() {
        return discoveryUri;
    }

    public String getTokenUri() {
        return tokenUri;
    }

    public String getRevocationUri() {
        return revocationUri;
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

    public String getRegistrationUri() {
        return registrationUri;
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
