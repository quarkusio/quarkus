package io.quarkus.oidc.runtime;

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import io.quarkus.oidc.OIDCException;
import io.quarkus.oidc.OidcConfigurationMetadata;
import io.quarkus.oidc.OidcProviderClient;
import io.quarkus.security.identity.SecurityIdentity;

@RequestScoped
public class OidcConfigurationAndProviderProducer {
    @Inject
    TenantConfigBean tenantConfig;
    @Inject
    SecurityIdentity identity;

    @Produces
    @RequestScoped
    OidcConfigurationMetadata produceMetadata() {
        OidcConfigurationMetadata configMetadata = OidcUtils.getAttribute(identity, OidcUtils.CONFIG_METADATA_ATTRIBUTE);

        if (configMetadata == null && tenantConfig.getDefaultTenant().oidcConfig().tenantEnabled()) {
            configMetadata = tenantConfig.getDefaultTenant().provider().getMetadata();
        }
        if (configMetadata == null) {
            throw new OIDCException("OidcConfigurationMetadata can not be injected");
        }
        return configMetadata;
    }

    @Produces
    @RequestScoped
    OidcProviderClient produceProviderClient() {
        OidcProviderClient client = null;
        String tenantId = OidcUtils.getAttribute(identity, OidcUtils.TENANT_ID_ATTRIBUTE);
        if (tenantId != null) {
            if (OidcUtils.DEFAULT_TENANT_ID.equals(tenantId)) {
                return tenantConfig.getDefaultTenant().getOidcProviderClient();
            }
            TenantConfigContext context = tenantConfig.getStaticTenant(tenantId);
            if (context == null) {
                context = tenantConfig.getDynamicTenant(tenantId);
            }
            if (context != null) {
                client = context.getOidcProviderClient();
            }
        }
        if (client == null) {
            throw new OIDCException("OidcProviderClient can not be injected");
        }
        return client;
    }
}
