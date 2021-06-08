package io.quarkus.it.keycloak;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.TenantConfigResolver;
import io.quarkus.oidc.common.runtime.OidcCommonConfig;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class CustomTenantConfigResolver implements TenantConfigResolver {

    @ConfigProperty(name = "admin-url")
    String url;

    @Override
    public OidcTenantConfig resolve(RoutingContext context) {
        if (context.request().path().endsWith("dynamic")) {
            OidcTenantConfig dynamicConfig = new OidcTenantConfig();
            dynamicConfig.setTenantId("dynamic-tenant");
            dynamicConfig.setClientId("quarkus-app");
            dynamicConfig.setApplicationType(OidcTenantConfig.ApplicationType.HYBRID);
            dynamicConfig.setAuthServerUrl(url + "/realms/quarkus");
            OidcCommonConfig.Credentials dynamicCreds = new OidcCommonConfig.Credentials();
            dynamicCreds.setSecret("secret");
            dynamicConfig.setCredentials(dynamicCreds);
            return dynamicConfig;
        }

        return null;
    }
}
