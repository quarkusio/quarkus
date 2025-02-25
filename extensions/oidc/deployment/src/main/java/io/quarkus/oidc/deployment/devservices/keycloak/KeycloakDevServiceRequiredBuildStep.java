package io.quarkus.oidc.deployment.devservices.keycloak;

import static io.quarkus.devservices.keycloak.KeycloakDevServicesRequiredBuildItem.OIDC_AUTH_SERVER_URL_CONFIG_KEY;

import java.util.HashMap;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.dev.devservices.DevServicesConfig;
import io.quarkus.devservices.keycloak.KeycloakDevServicesConfig;
import io.quarkus.devservices.keycloak.KeycloakDevServicesRequiredBuildItem;
import io.quarkus.oidc.deployment.OidcBuildStep;

@BuildSteps(onlyIfNot = IsNormal.class, onlyIf = { OidcBuildStep.IsEnabled.class, DevServicesConfig.Enabled.class })
public class KeycloakDevServiceRequiredBuildStep {

    private static final Logger LOG = Logger.getLogger(KeycloakDevServiceRequiredBuildStep.class);
    private static final String CONFIG_PREFIX = "quarkus.oidc.";
    private static final String TENANT_ENABLED_CONFIG_KEY = CONFIG_PREFIX + "tenant-enabled";
    private static final String APPLICATION_TYPE_CONFIG_KEY = CONFIG_PREFIX + "application-type";
    private static final String CLIENT_ID_CONFIG_KEY = CONFIG_PREFIX + "client-id";
    private static final String CLIENT_SECRET_CONFIG_KEY = CONFIG_PREFIX + "credentials.secret";

    @BuildStep
    KeycloakDevServicesRequiredBuildItem requireKeycloakDevService(KeycloakDevServicesConfig config) {
        if (!isOidcTenantEnabled() && !config.startWithDisabledTenant()) {
            LOG.debug("Not starting Dev Services for Keycloak as 'quarkus.oidc.tenant.enabled' is false");
            return null;
        }

        return KeycloakDevServicesRequiredBuildItem.of(ctx -> {
            var configProperties = new HashMap<String, String>();
            configProperties.put(OIDC_AUTH_SERVER_URL_CONFIG_KEY, ctx.authServerInternalUrl());
            configProperties.put(APPLICATION_TYPE_CONFIG_KEY, getOidcApplicationType());
            if (config.createClient()) {
                configProperties.put(CLIENT_ID_CONFIG_KEY, ctx.oidcClientId());
                configProperties.put(CLIENT_SECRET_CONFIG_KEY, ctx.oidcClientSecret());
            }
            return configProperties;
        }, OIDC_AUTH_SERVER_URL_CONFIG_KEY);
    }

    private static boolean isOidcTenantEnabled() {
        return ConfigProvider.getConfig().getOptionalValue(TENANT_ENABLED_CONFIG_KEY, Boolean.class).orElse(true);
    }

    private static String getOidcApplicationType() {
        return ConfigProvider.getConfig().getOptionalValue(APPLICATION_TYPE_CONFIG_KEY, String.class).orElse("service");
    }
}
