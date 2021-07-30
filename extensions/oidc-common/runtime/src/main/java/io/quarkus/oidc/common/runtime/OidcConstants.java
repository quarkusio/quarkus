package io.quarkus.oidc.common.runtime;

public final class OidcConstants {

    public static final String WELL_KNOWN_CONFIGURATION = "/.well-known/openid-configuration";

    public static final String CLIENT_ASSERTION = "client_assertion";
    public static final String CLIENT_ASSERTION_TYPE = "client_assertion_type";
    public static final String JWT_BEARER_CLIENT_ASSERTION_TYPE = "urn:ietf:params:oauth:client-assertion-type:jwt-bearer";

    public static final String CLIENT_CREDENTIALS_GRANT = "client_credentials";
    public static final String PASSWORD_GRANT = "password";
    public static final String REFRESH_TOKEN_GRANT = "refresh_token";
    public static final String REFRESH_TOKEN_VALUE = "refresh_token";

    public static final String ACCESS_TOKEN_VALUE = "access_token";
    public static final String ID_TOKEN_VALUE = "id_token";

    public static final String INTROSPECTION_TOKEN_TYPE_HINT = "token_type_hint";
    public static final String INTROSPECTION_TOKEN = "token";
    public static final String INTROSPECTION_TOKEN_ACTIVE = "active";
    public static final String INTROSPECTION_TOKEN_EXP = "exp";
    public static final String INTROSPECTION_TOKEN_USERNAME = "username";
    public static final String INTROSPECTION_TOKEN_SUB = "sub";

    public static final String PASSWORD_GRANT_USERNAME = "username";
    public static final String PASSWORD_GRANT_PASSWORD = "password";

    public static final String TOKEN_SCOPE = "scope";
    public static final String GRANT_TYPE = "grant_type";

    public static final String CLIENT_ID = "client_id";
    public static final String CLIENT_SECRET = "client_secret";

    public static final String BEARER_SCHEME = "Bearer";
    public static final String BASIC_SCHEME = "Basic";

    public static final String AUTHORIZATION_CODE = "authorization_code";
    public static final String CODE_FLOW_RESPONSE_TYPE = "response_type";
    public static final String CODE_FLOW_CODE = "code";
    public static final String CODE_FLOW_STATE = "state";
    public static final String CODE_FLOW_REDIRECT_URI = "redirect_uri";

    public static final String EXCHANGE_GRANT = "urn:ietf:params:oauth:grant-type:token-exchange";

    public static final String EXPIRES_IN = "expires_in";
}
