package io.quarkus.it.keycloak;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.oidc.OidcRequestContext;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.TenantConfigResolver;
import io.quarkus.oidc.runtime.OidcUtils;
import io.quarkus.oidc.runtime.TenantConfigBean;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class CustomTenantConfigResolver implements TenantConfigResolver {

    @Inject
    @ConfigProperty(name = "keycloak.url")
    String keycloakUrl;

    @Inject
    TenantConfigBean tenantConfigBean;

    @Override
    public Uni<OidcTenantConfig> resolve(RoutingContext context,
            OidcRequestContext<OidcTenantConfig> requestContext) {
        String path = context.normalizedPath();
        // `/hr-classic-perm-check` and '/hr-classic-and-jaxrs-perm-check'
        // require policy checks which force an authentication before @Tenant is resolved
        if (path.contains("/hr") && !path.contains("/hr-classic-perm-check")
                && !path.contains("/hr-classic-and-jaxrs-perm-check")) {
            throw new RuntimeException("@Tenant annotation only must be used to set "
                    + "a tenant id on the '" + path + "' request path");
        }
        if (context.get(OidcUtils.TENANT_ID_ATTRIBUTE) != null) {
            if (context.get(OidcUtils.TENANT_ID_SET_BY_ANNOTATION) != null) {
                throw new RuntimeException(
                        "Calling TenantConfigResolver after @Tenant has already resolved tenant id is unnecessary");
            }
            if (context.get(OidcUtils.TENANT_ID_SET_BY_SESSION_COOKIE) == null
                    && context.get(OidcUtils.TENANT_ID_SET_BY_STATE_COOKIE) == null) {
                throw new RuntimeException("Tenant id must have been set by either the session or state cookie");
            }
            // Expect an already resolved tenant context be used
            if (path.endsWith("code-flow-user-info-dynamic-github")) {
                context.put("tenant-config-resolver", "true");

                List<String> update = context.queryParam("update");
                if (update != null && !update.isEmpty() && "true".equals(update.get(0))) {
                    var currentTenantConfig = tenantConfigBean.getDynamicTenant("code-flow-user-info-dynamic-github")
                            .getOidcTenantConfig();
                    if ("name".equals(currentTenantConfig.token().principalClaim().get())) {
                        // This is the original config
                        OidcTenantConfig updatedConfig = OidcTenantConfig.builder(currentTenantConfig)
                                .token().principalClaim("email").end()
                                .resourceMetadata().resource("github").end()
                                .build();
                        return Uni.createFrom().item(updatedConfig);
                    }
                }

                List<String> reconnect = context.queryParam("reconnect");
                if (reconnect != null && !reconnect.isEmpty() && "true".equals(reconnect.get(0))) {

                    var currentTenantConfig = tenantConfigBean.getDynamicTenant("code-flow-user-info-dynamic-github")
                            .getOidcTenantConfig();
                    if ("email".equals(currentTenantConfig.token().principalClaim().get())) {
                        // This is the config created at the update step
                        OidcTenantConfig updatedConfig = OidcTenantConfig.builder(currentTenantConfig)
                                .authServerUrl(keycloakUrl + "/realms/github/")
                                .provider(null)
                                .discoveryEnabled(true)
                                .authorizationPath(null)
                                .userInfoPath(null)
                                .tokenPath(null)
                                .token().principalClaim("personal-email").end()
                                .build();

                        context.put("replace-tenant-configuration-context", "true");
                        context.put("remove-session-cookie", "true");
                        return Uni.createFrom().item(updatedConfig);
                    }
                }
            }
            return null;
        }
        if (path.endsWith("code-flow-user-info-dynamic-github")) {
            OidcTenantConfig config = OidcTenantConfig.authServerUrl(keycloakUrl + "/realms/quarkus")
                    .clientId("quarkus-web-app")
                    .credentials("AyM1SysPpbyDfgZld3umj1qzKObwVMkoqQ-EstJQLr_T-1qS0gZH75aKtMN3Yj0iPS4hcgUuTwjAzZr1Z9CAow")
                    .tenantId("code-flow-user-info-dynamic-github")
                    .provider(io.quarkus.oidc.runtime.OidcTenantConfig.Provider.GITHUB)
                    .authorizationPath("/")
                    .userInfoPath("protocol/openid-connect/userinfo")
                    .codeGrant()
                    .header("X-Custom", "XCustomHeaderValue")
                    .extraParam("extra-param", "extra-param-value")
                    .end()
                    .allowUserInfoCache(false)
                    .authentication().internalIdTokenLifespan(Duration.ofSeconds(301)).end()
                    .resourceMetadata().enabled().end()
                    .build();

            return Uni.createFrom().item(config);
        } else if (path.endsWith("bearer-certificate-full-chain-root-only")) {
            OidcTenantConfig config = new OidcTenantConfig();
            config.setTenantId("bearer-certificate-full-chain-root-only");
            config.getCertificateChain().setTrustStoreFile(Path.of("target/chain/truststore-rootcert.p12"));
            config.getCertificateChain().setTrustStorePassword("storepassword");
            config.getCertificateChain().setLeafCertificateName("www.quarkustest.com");
            return Uni.createFrom().item(config);
        }

        return Uni.createFrom().nullItem();
    }

}
