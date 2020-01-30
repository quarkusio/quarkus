package io.quarkus.it.keycloak;

import javax.enterprise.context.ApplicationScoped;

import io.quarkus.oidc.TenantConfigResolver;
import io.quarkus.oidc.runtime.OidcTenantConfig;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class CustomTenantConfigResolver implements TenantConfigResolver {
    @Override
    public OidcTenantConfig resolve(RoutingContext context) {
        if ("tenant-d".equals(context.request().path().split("/")[2])) {
            OidcTenantConfig config = new OidcTenantConfig();

            config.setAuthServerUrl(getIssuerUrl() + "/realms/quarkus-d");
            config.setClientId("quarkus-d");
            OidcTenantConfig.Credentials credentials = new OidcTenantConfig.Credentials();

            credentials.setSecret("secret");

            config.setCredentials(credentials);

            OidcTenantConfig.Token token = new OidcTenantConfig.Token();

            token.setIssuer(getIssuerUrl() + "/realms/quarkus-d");

            config.setToken(token);

            return config;
        }
        return null;
    }

    private String getIssuerUrl() {
        return System.getProperty("keycloak.url", "http://localhost:8180/auth");
    }
}
