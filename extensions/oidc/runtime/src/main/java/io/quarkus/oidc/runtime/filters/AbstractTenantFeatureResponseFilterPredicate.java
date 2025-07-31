package io.quarkus.oidc.runtime.filters;

import java.util.List;

import io.quarkus.oidc.common.OidcResponseFilter;
import io.quarkus.oidc.common.runtime.OidcFilterStorage;
import io.quarkus.oidc.runtime.OidcUtils;

public abstract class AbstractTenantFeatureResponseFilterPredicate implements OidcFilterStorage.OidcResponseContextPredicate {

    protected abstract List<String> getTenantIds();

    @Override
    public boolean test(OidcResponseFilter.OidcResponseContext oidcResponseContext) {
        if (oidcResponseContext.requestProperties() == null) {
            return false;
        }
        return applyTo(oidcResponseContext.requestProperties().getString(OidcUtils.TENANT_ID_ATTRIBUTE));
    }

    private boolean applyTo(String tenantId) {
        if (tenantId == null) {
            return false;
        }
        return getTenantIds().contains(tenantId);
    }
}
