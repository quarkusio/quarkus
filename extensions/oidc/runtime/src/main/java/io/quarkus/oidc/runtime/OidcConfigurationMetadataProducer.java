package io.quarkus.oidc.runtime;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import io.quarkus.oidc.OIDCException;
import io.quarkus.oidc.OidcConfigurationMetadata;
import io.quarkus.security.identity.SecurityIdentity;

@RequestScoped
public class OidcConfigurationMetadataProducer {
    @Inject
    TenantConfigBean tenantConfig;
    @Inject
    SecurityIdentity identity;

    @Produces
    @RequestScoped
    OidcConfigurationMetadata produce() {
        OidcConfigurationMetadata configMetadata = null;

        configMetadata = (OidcConfigurationMetadata) identity.getAttribute(OidcUtils.CONFIG_METADATA_ATTRIBUTE);

        if (configMetadata == null && tenantConfig.getDefaultTenant().oidcConfig.tenantEnabled) {
            configMetadata = tenantConfig.getDefaultTenant().provider.getMetadata();
        }
        if (configMetadata == null) {
            throw new OIDCException("OidcConfigurationMetadata can not be injected");
        }
        return configMetadata;
    }
}
