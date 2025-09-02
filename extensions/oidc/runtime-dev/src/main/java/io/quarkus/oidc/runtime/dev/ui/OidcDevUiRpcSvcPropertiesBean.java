package io.quarkus.oidc.runtime.dev.ui;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class OidcDevUiRpcSvcPropertiesBean {

    private final String authorizationUrl;
    private final String tokenUrl;
    private final String logoutUrl;
    private final Duration webClientTimeout;
    private final Map<String, Map<String, String>> grantOptions;
    private final Map<String, String> oidcUsers;
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
    /**
     * Properties state id helps UI to determine that properties may have changed
     * and web component needs to be updated.
     */
    private final String propertiesStateId;
    private final String logoutPath;
    private final String readSessionCookiePath;

    OidcDevUiRpcSvcPropertiesBean(String authorizationUrl, String tokenUrl, String logoutUrl,
            Duration webClientTimeout, Map<String, Map<String, String>> grantOptions,
            Map<String, String> oidcUsers, String oidcProviderName, String oidcApplicationType,
            String oidcGrantType, boolean introspectionIsAvailable, String keycloakAdminUrl,
            List<String> keycloakRealms, boolean swaggerIsAvailable, boolean graphqlIsAvailable,
            String swaggerUiPath, String graphqlUiPath, boolean alwaysLogoutUserInDevUiOnReload,
            String devUiLogoutPath, String devUiReadSessionCookiePath) {
        this.authorizationUrl = authorizationUrl;
        this.tokenUrl = tokenUrl;
        this.logoutUrl = logoutUrl;
        this.webClientTimeout = webClientTimeout;
        this.grantOptions = Map.copyOf(grantOptions);
        this.oidcUsers = oidcUsers == null || oidcUsers.isEmpty() ? Map.of() : Map.copyOf(oidcUsers);
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
        this.propertiesStateId = Long.toString(UUID.randomUUID().getLeastSignificantBits());
        this.logoutPath = devUiLogoutPath;
        this.readSessionCookiePath = devUiReadSessionCookiePath;
    }

    String getAuthorizationUrl() {
        return authorizationUrl;
    }

    String getTokenUrl() {
        return tokenUrl;
    }

    String getLogoutUrl() {
        return logoutUrl;
    }

    Duration getWebClientTimeout() {
        return webClientTimeout;
    }

    Map<String, String> getCodeGrantOptions() {
        return grantOptions.get("code");
    }

    Map<String, String> getPasswordGrantOptions() {
        return grantOptions.get("password");
    }

    Map<String, String> getClientCredGrantOptions() {
        return grantOptions.get("client");
    }

    Map<String, String> getOidcUsers() {
        return oidcUsers;
    }

    boolean isIntrospectionIsAvailable() {
        return introspectionIsAvailable;
    }

    String getKeycloakAdminUrl() {
        return keycloakAdminUrl;
    }

    List<String> getKeycloakRealms() {
        return keycloakRealms;
    }

    boolean isSwaggerIsAvailable() {
        return swaggerIsAvailable;
    }

    boolean isGraphqlIsAvailable() {
        return graphqlIsAvailable;
    }

    String getSwaggerUiPath() {
        return swaggerUiPath;
    }

    String getGraphqlUiPath() {
        return graphqlUiPath;
    }

    String getOidcProviderName() {
        return oidcProviderName;
    }

    String getOidcApplicationType() {
        return oidcApplicationType;
    }

    String getOidcGrantType() {
        return oidcGrantType;
    }

    boolean isAlwaysLogoutUserInDevUiOnReload() {
        return alwaysLogoutUserInDevUiOnReload;
    }

    String getPropertiesStateId() {
        return propertiesStateId;
    }

    String getLogoutPath() {
        return logoutPath;
    }

    String getReadSessionCookiePath() {
        return readSessionCookiePath;
    }
}
