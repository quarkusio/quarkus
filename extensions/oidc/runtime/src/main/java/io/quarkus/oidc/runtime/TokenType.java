package io.quarkus.oidc.runtime;

/**
 * Represents the type of token being verified during OIDC authentication.
 */
enum TokenType {
    /**
     * Bearer access token from HTTP Authorization header.
     */
    BEARER_ACCESS_TOKEN,

    /**
     * Access token obtained via authorization code flow.
     */
    CODE_FLOW_ACCESS_TOKEN,

    /**
     * ID token from authorization code flow.
     */
    ID_TOKEN
}
