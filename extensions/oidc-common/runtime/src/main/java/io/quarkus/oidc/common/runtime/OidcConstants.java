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

    public static final String LOGOUT_ID_TOKEN_HINT = "id_token_hint";
    public static final String LOGOUT_STATE = "state";
    public static final String POST_LOGOUT_REDIRECT_URI = "post_logout_redirect_uri";

    public static final String INTROSPECTION_TOKEN_TYPE_HINT = "token_type_hint";
    public static final String INTROSPECTION_TOKEN = "token";
    public static final String INTROSPECTION_TOKEN_ACTIVE = "active";
    public static final String INTROSPECTION_TOKEN_CLIENT_ID = "client_id";
    public static final String INTROSPECTION_TOKEN_EXP = "exp";
    public static final String INTROSPECTION_TOKEN_IAT = "iat";
    public static final String INTROSPECTION_TOKEN_USERNAME = "username";
    public static final String INTROSPECTION_TOKEN_SUB = "sub";
    public static final String INTROSPECTION_TOKEN_AUD = "aud";
    public static final String INTROSPECTION_TOKEN_ISS = "iss";

    public static final String REVOCATION_TOKEN = "token";

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
    public static final String CODE_FLOW_RESPONSE_MODE = "response_mode";
    public static final String CODE_FLOW_CODE = "code";
    public static final String CODE_FLOW_ERROR = "error";
    public static final String CODE_FLOW_ERROR_DESCRIPTION = "error_description";
    public static final String CODE_FLOW_STATE = "state";
    public static final String CODE_FLOW_REDIRECT_URI = "redirect_uri";

    public static final String EXCHANGE_GRANT = "urn:ietf:params:oauth:grant-type:token-exchange";

    public static final String EXPIRES_IN = "expires_in";
    public static final String REFRESH_EXPIRES_IN = "refresh_expires_in";

    public static final String PKCE_CODE_VERIFIER = "code_verifier";
    public static final String PKCE_CODE_CHALLENGE = "code_challenge";

    public static final String PKCE_CODE_CHALLENGE_METHOD = "code_challenge_method";
    public static final String PKCE_CODE_CHALLENGE_S256 = "S256";

    public static final String BACK_CHANNEL_LOGOUT_TOKEN = "logout_token";
    public static final String BACK_CHANNEL_EVENTS_CLAIM = "events";
    public static final String BACK_CHANNEL_EVENT_NAME = "http://schemas.openid.net/event/backchannel-logout";
    public static final String BACK_CHANNEL_LOGOUT_SID_CLAIM = "sid";
    public static final String FRONT_CHANNEL_LOGOUT_SID_PARAM = "sid";
    public static final String ID_TOKEN_SID_CLAIM = "sid";

    public static final String OPENID_SCOPE = "openid";
    public static final String NONCE = "nonce";
}
