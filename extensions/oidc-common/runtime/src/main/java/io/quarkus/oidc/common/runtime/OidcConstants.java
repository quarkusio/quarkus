package io.quarkus.oidc.common.runtime;

public final class OidcConstants {

    public static final String CLIENT_ASSERTION = "client_assertion";
    public static final String CLIENT_ASSERTION_TYPE = "client_assertion_type";
    public static final String JWT_BEARER_CLIENT_ASSERTION_TYPE = "urn:ietf:params:oauth:client-assertion-type:jwt-bearer";

    public static final String CLIENT_CREDENTIALS_GRANT = "client_credentials";
    public static final String PASSWORD_GRANT = "password";
    public static final String REFRESH_TOKEN_GRANT = "refresh_token";

    public static final String PASSWORD_GRANT_USERNAME = "username";
    public static final String PASSWORD_GRANT_PASSWORD = "password";

    public static final String TOKEN_SCOPE = "scope";
    public static final String GRANT_TYPE = "grant_type";

    public static final String CLIENT_ID = "client_id";
    public static final String CLIENT_SECRET = "client_secret";

    public static final String BEARER_SCHEME = "Bearer";

    public static final String EXPIRES_IN = "expires_in";
}
