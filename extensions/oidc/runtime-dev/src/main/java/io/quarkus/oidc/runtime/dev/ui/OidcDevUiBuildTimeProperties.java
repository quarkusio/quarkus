package io.quarkus.oidc.runtime.dev.ui;

import java.time.Duration;
import java.util.Map;

public record OidcDevUiBuildTimeProperties(Duration webClientTimeout,
        Map<String, Map<String, String>> grantOptions,
        String oidcProviderName, String oidcGrantType, boolean introspectionIsAvailable, boolean swaggerIsAvailable,
        boolean graphqlIsAvailable, String swaggerUiPath, String graphqlUiPath, String devServiceConfigHashCode,
        boolean discoverMetadata, String devUiLogoutPath, String devUiReadSessionCookiePath, String authServerUrl,
        String buildTimeKeycloakAdminUrl, String buildTimeOidcApplicationType, DevServiceType devServiceType) {

    public enum DevServiceType {
        KEYCLOAK,
        OIDC,
        OTHER
    }

}
