package io.quarkus.oidc.runtime.filters;

import java.util.List;

import io.quarkus.oidc.common.OidcRequestFilter;
import io.quarkus.oidc.common.runtime.OidcFilterStorage;
import io.quarkus.oidc.runtime.OidcUtils;

public abstract class AbstractTenantFeatureRequestFilterPredicate implements OidcFilterStorage.OidcRequestContextPredicate {

    protected abstract List<String> getTenantIds();

    @Override
    public boolean test(OidcRequestFilter.OidcRequestContext oidcRequestContext) {
        if (oidcRequestContext.contextProperties() == null) {
            return false;
        }
        return applyTo(oidcRequestContext.contextProperties().getString(OidcUtils.TENANT_ID_ATTRIBUTE));
    }

    private boolean applyTo(String tenantId) {
        if (tenantId == null) {
            return false;
        }
        return getTenantIds().contains(tenantId);
    }
}
