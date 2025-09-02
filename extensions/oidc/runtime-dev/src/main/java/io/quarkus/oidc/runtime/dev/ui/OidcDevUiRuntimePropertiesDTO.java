package io.quarkus.oidc.runtime.dev.ui;

import java.util.List;

import org.eclipse.microprofile.config.Config;

import io.quarkus.oidc.runtime.OidcConfigPropertySupplier;

public class OidcDevUiRuntimePropertiesDTO {
    private static final String CONFIG_PREFIX = "quarkus.oidc.";
    private static final String CLIENT_ID_CONFIG_KEY = CONFIG_PREFIX + "client-id";
    private static final String CLIENT_SECRET_CONFIG_KEY = CONFIG_PREFIX + "credentials.secret";
    private static final String AUTHORIZATION_PATH_CONFIG_KEY = CONFIG_PREFIX + "authorization-path";
    private static final String TOKEN_PATH_CONFIG_KEY = CONFIG_PREFIX + "token-path";
    private static final String END_SESSION_PATH_CONFIG_KEY = CONFIG_PREFIX + "end-session-path";
    private static final String POST_LOGOUT_URI_PARAM_CONFIG_KEY = CONFIG_PREFIX + "logout.post-logout-uri-param";
    private static final String SCOPES_KEY = CONFIG_PREFIX + "authentication.scopes";
    private static final String AUTH_EXTRA_PARAMS_KEY = CONFIG_PREFIX + "authentication.extra-params";
    private final String clientId;
    private final String clientSecret;
    private final String authorizationUrl;
    private final String tokenUrl;
    private final String logoutUrl;
    private final String postLogoutUriParam;
    private final String scopes;
    private final String authExtraParams;
    private final int httpPort;
    private final String oidcProviderName;
    private final String oidcApplicationType;
    private final String oidcGrantType;
    private final boolean introspectionIsAvailable;
    private final String keycloakAdminUrl;
    private final List<String> keycloakRealms;
    private final boolean swaggerIsAvailable;
    private final boolean graphqlIsAvailable;
    private final String swaggerUiPath;
    private final String graphqlUiPath;
    private final boolean alwaysLogoutUserInDevUiOnReload;
    private final String propertiesStateId;
    private final String logoutPath;
    private final String readSessionCookiePath;

    OidcDevUiRuntimePropertiesDTO(String authorizationUrl, String tokenUrl, String logoutUrl, Config config, int httpPort,
            String oidcProviderName, String oidcApplicationType, String oidcGrantType,
            boolean introspectionIsAvailable, String keycloakAdminUrl, List<String> keycloakRealms,
            boolean swaggerIsAvailable, boolean graphqlIsAvailable, String swaggerUiPath,
            String graphqlUiPath, boolean alwaysLogoutUserInDevUiOnReload, String propertiesStateId,
            String logoutPath, String readSessionCookiePath) {
        this.clientId = new OidcConfigPropertySupplier(CLIENT_ID_CONFIG_KEY).get(config);
        this.clientSecret = new OidcConfigPropertySupplier(CLIENT_SECRET_CONFIG_KEY, "").get(config);
        this.authorizationUrl = new OidcConfigPropertySupplier(AUTHORIZATION_PATH_CONFIG_KEY, authorizationUrl, true)
                .get(config);
        this.tokenUrl = new OidcConfigPropertySupplier(TOKEN_PATH_CONFIG_KEY, tokenUrl, true).get(config);
        this.logoutUrl = new OidcConfigPropertySupplier(END_SESSION_PATH_CONFIG_KEY, logoutUrl, true).get(config);
        this.postLogoutUriParam = new OidcConfigPropertySupplier(POST_LOGOUT_URI_PARAM_CONFIG_KEY).get(config);
        this.scopes = new OidcConfigPropertySupplier(SCOPES_KEY).get(config);
        this.authExtraParams = new OidcConfigPropertySupplier(AUTH_EXTRA_PARAMS_KEY).get(config);
        this.httpPort = httpPort;
        this.oidcProviderName = oidcProviderName;
        this.oidcApplicationType = oidcApplicationType;
        this.oidcGrantType = oidcGrantType;
        this.introspectionIsAvailable = introspectionIsAvailable;
        this.keycloakAdminUrl = keycloakAdminUrl;
        this.keycloakRealms = keycloakRealms;
        this.swaggerIsAvailable = swaggerIsAvailable;
        this.graphqlIsAvailable = graphqlIsAvailable;
        this.swaggerUiPath = swaggerUiPath;
        this.graphqlUiPath = graphqlUiPath;
        this.alwaysLogoutUserInDevUiOnReload = alwaysLogoutUserInDevUiOnReload;
        this.propertiesStateId = propertiesStateId;
        this.logoutPath = logoutPath;
        this.readSessionCookiePath = readSessionCookiePath;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getAuthorizationUrl() {
        return authorizationUrl;
    }

    public String getTokenUrl() {
        return tokenUrl;
    }

    public String getLogoutUrl() {
        return logoutUrl;
    }

    public String getPostLogoutUriParam() {
        return postLogoutUriParam;
    }

    public String getScopes() {
        return scopes;
    }

    public String getAuthExtraParams() {
        return authExtraParams;
    }

    public int getHttpPort() {
        return httpPort;
    }

    public String getOidcProviderName() {
        return oidcProviderName;
    }

    public String getOidcApplicationType() {
        return oidcApplicationType;
    }

    public String getOidcGrantType() {
        return oidcGrantType;
    }

    public boolean isIntrospectionIsAvailable() {
        return introspectionIsAvailable;
    }

    public String getKeycloakAdminUrl() {
        return keycloakAdminUrl;
    }

    public List<String> getKeycloakRealms() {
        return keycloakRealms;
    }

    public boolean isSwaggerIsAvailable() {
        return swaggerIsAvailable;
    }

    public boolean isGraphqlIsAvailable() {
        return graphqlIsAvailable;
    }

    public String getSwaggerUiPath() {
        return swaggerUiPath;
    }

    public String getGraphqlUiPath() {
        return graphqlUiPath;
    }

    public boolean isAlwaysLogoutUserInDevUiOnReload() {
        return alwaysLogoutUserInDevUiOnReload;
    }

    public String getPropertiesStateId() {
        return propertiesStateId;
    }

    public String getLogoutPath() {
        return logoutPath;
    }

    public String getReadSessionCookiePath() {
        return readSessionCookiePath;
    }
}
