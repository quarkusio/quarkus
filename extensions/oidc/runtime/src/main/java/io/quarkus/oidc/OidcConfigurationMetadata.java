package io.quarkus.oidc;

import io.vertx.core.json.JsonObject;

public class OidcConfigurationMetadata {
    private final String tokenUri;
    private final String introspectionUri;
    private final String authorizationUri;
    private final String jsonWebKeySetUri;
    private final String userInfoUri;
    private final String endSessionUri;
    private final String issuer;

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
    }

    public OidcConfigurationMetadata(JsonObject wellKnownConfig) {
        this.tokenUri = wellKnownConfig.getString("token_endpoint");
        this.introspectionUri = wellKnownConfig.getString("token_introspection_endpoint");
        this.authorizationUri = wellKnownConfig.getString("authorization_endpoint");
        this.jsonWebKeySetUri = wellKnownConfig.getString("jwks_uri");
        this.userInfoUri = wellKnownConfig.getString("userinfo_endpoint");
        this.endSessionUri = wellKnownConfig.getString("end_session_endpoint");
        this.issuer = wellKnownConfig.getString("issuer");
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

    public String getIssuer() {
        return issuer;
    }
}
