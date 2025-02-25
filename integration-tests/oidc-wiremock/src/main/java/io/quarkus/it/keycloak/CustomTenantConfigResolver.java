package io.quarkus.it.keycloak;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.oidc.OidcRequestContext;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.OidcTenantConfig.Provider;
import io.quarkus.oidc.TenantConfigResolver;
import io.quarkus.oidc.runtime.OidcUtils;
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
            }
            return null;
        }
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
            config.getCodeGrant().setHeaders(Map.of("X-Custom", "XCustomHeaderValue"));
            config.getCodeGrant().setExtraParams(Map.of("extra-param", "extra-param-value"));
            config.getAuthentication().setInternalIdTokenLifespan(Duration.ofSeconds(301));
            config.setAllowUserInfoCache(false);
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
