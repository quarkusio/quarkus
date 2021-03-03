package io.quarkus.oidc;

import io.vertx.core.json.JsonObject;

public class OidcConfigurationMetadata {
    private static final String ISSUER = "issuer";
    private static final String TOKEN_ENDPOINT = "token_endpoint";
    private static final String INTROSPECTION_ENDPOINT = "introspection_endpoint";
    private static final String AUTHORIZATION_ENDPOINT = "authorization_endpoint";
    private static final String JWKS_ENDPOINT = "jwks_uri";
    private static final String USERINFO_ENDPOINT = "userinfo_endpoint";
    private static final String END_SESSION_ENDPOINT = "end_session_endpoint";

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
        this.tokenUri = wellKnownConfig.getString(TOKEN_ENDPOINT);
        this.introspectionUri = wellKnownConfig.getString(INTROSPECTION_ENDPOINT);
        this.authorizationUri = wellKnownConfig.getString(AUTHORIZATION_ENDPOINT);
        this.jsonWebKeySetUri = wellKnownConfig.getString(JWKS_ENDPOINT);
        this.userInfoUri = wellKnownConfig.getString(USERINFO_ENDPOINT);
        this.endSessionUri = wellKnownConfig.getString(END_SESSION_ENDPOINT);
        this.issuer = wellKnownConfig.getString(ISSUER);
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
