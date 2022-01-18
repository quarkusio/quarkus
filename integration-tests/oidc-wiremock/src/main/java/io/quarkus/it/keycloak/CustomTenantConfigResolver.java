package io.quarkus.it.keycloak;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.oidc.OidcRequestContext;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.OidcTenantConfig.Provider;
import io.quarkus.oidc.TenantConfigResolver;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class CustomTenantConfigResolver implements TenantConfigResolver {

    @Inject
    @ConfigProperty(name = "keycloak.url")
    String keycloakUrl;

    @Override
    public Uni<OidcTenantConfig> resolve(RoutingContext context,
            OidcRequestContext<OidcTenantConfig> requestContext) {
        String path = context.normalizedPath();
        if (path.endsWith("code-flow-user-info-dynamic-github")) {

            OidcTenantConfig config = new OidcTenantConfig();
            config.setTenantId("code-flow-user-info-dynamic-github");

            config.setProvider(Provider.GITHUB);

            config.setAuthServerUrl(keycloakUrl + "/realms/quarkus/");
            config.setAuthorizationPath("/");
            config.setUserInfoPath("protocol/openid-connect/userinfo");
            config.setClientId("quarkus-web-app");
            config.getCredentials()
                    .setSecret("AyM1SysPpbyDfgZld3umj1qzKObwVMkoqQ-EstJQLr_T-1qS0gZH75aKtMN3Yj0iPS4hcgUuTwjAzZr1Z9CAow");

            return Uni.createFrom().item(config);
        }

        return Uni.createFrom().nullItem();
    }

}
