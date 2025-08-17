package io.quarkus.it.keycloak;

import jakarta.inject.Singleton;

import io.quarkus.oidc.TenantFeature;
import io.quarkus.oidc.common.OidcEndpoint;
import io.quarkus.oidc.common.OidcResponseFilter;
import io.quarkus.oidc.runtime.OidcUtils;

@OidcEndpoint(OidcEndpoint.Type.INTROSPECTION)
@Singleton
@TenantFeature("tenant-introspection-multiple-required-claims")
public class CustomTenantFeatureAuthResponseFilter implements OidcResponseFilter {

    private volatile String tenantId = null;

    public String getAndReset() {
        String currentState = tenantId;
        tenantId = null;
        return currentState == null ? "" : currentState;
    }

    @Override
    public void filter(OidcResponseContext responseContext) {
        tenantId = responseContext.requestProperties().getString(OidcUtils.TENANT_ID_ATTRIBUTE);
    }
}
