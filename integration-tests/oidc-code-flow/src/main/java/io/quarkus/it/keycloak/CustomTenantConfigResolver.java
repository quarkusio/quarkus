package io.quarkus.it.keycloak;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.oidc.OidcRequestContext;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.OidcTenantConfig.ApplicationType;
import io.quarkus.oidc.TenantConfigResolver;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class CustomTenantConfigResolver implements TenantConfigResolver {

    @Inject
    @ConfigProperty(name = "quarkus.oidc.auth-server-url")
    String authServerUrl;

    OidcTenantConfig config = new OidcTenantConfig();

    public CustomTenantConfigResolver() {
    }

    @PostConstruct
    public void initConfig() {
        config.setTenantId("tenant-before-wrong-redirect");
        config.setAuthServerUrl(authServerUrl);
        config.setClientId("quarkus-app");
        config.getCredentials().setSecret("secret");
        config.setApplicationType(ApplicationType.WEB_APP);
    }

    @Override
    public Uni<OidcTenantConfig> resolve(RoutingContext context, OidcRequestContext<OidcTenantConfig> requestContext) {
        if (context.request().path().contains("callback-before-wrong-redirect")) {
            if (context.getCookie("q_auth_tenant-before-wrong-redirect") != null) {
                // trigger the code to access token exchange failure due to a redirect uri mismatch
                config.authentication.setRedirectPath("wrong-path");
            }
            return Uni.createFrom().item(config);
        }
        return Uni.createFrom().nullItem();
    }
}
