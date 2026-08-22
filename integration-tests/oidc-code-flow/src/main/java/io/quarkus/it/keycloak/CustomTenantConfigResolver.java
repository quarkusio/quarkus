package io.quarkus.it.keycloak;

import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.oidc.OidcRequestContext;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.TenantConfigResolver;
import io.quarkus.oidc.runtime.OidcTenantConfig.ApplicationType;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class CustomTenantConfigResolver implements TenantConfigResolver {

    @Inject
    @ConfigProperty(name = "quarkus.oidc.auth-server-url")
    String authServerUrl;

    OidcTenantConfig config;

    public CustomTenantConfigResolver() {
    }

    @PostConstruct
    public void initConfig() {
        config = OidcTenantConfig.authServerUrl(authServerUrl)
                .tenantId("tenant-before-wrong-redirect")
                .clientId("quarkus-app")
                .credentials().secret("secret").end()
                .applicationType(ApplicationType.WEB_APP)
                .build();
    }

    @Override
    public Uni<OidcTenantConfig> resolve(RoutingContext context, OidcRequestContext<OidcTenantConfig> requestContext) {
        if (context.request().path().contains("callback-before-wrong-redirect")) {
            List<String> stateParam = context.queryParam("state");
            if (stateParam.size() == 1 &&
                    context.request().getCookie("q_auth_tenant-before-wrong-redirect_" + stateParam.get(0)) != null) {
                // trigger the code to access token exchange failure due to a redirect uri mismatch
                config.authentication.setRedirectPath("wrong-path");
            }
            return Uni.createFrom().item(config);
        }
        return Uni.createFrom().nullItem();
    }

}
