package io.quarkus.it.keycloak;

import jakarta.inject.Singleton;

import io.quarkus.oidc.BearerTokenAuthentication;
import io.quarkus.oidc.common.OidcRequestFilter;
import io.quarkus.oidc.runtime.OidcUtils;

@Singleton
@BearerTokenAuthentication
public class CustomBearerTokenAuthRequestFilter implements OidcRequestFilter {

    private volatile String tenantId = null;

    @Override
    public void filter(OidcRequestContext requestContext) {
        tenantId = requestContext.contextProperties().getString(OidcUtils.TENANT_ID_ATTRIBUTE);
    }

    public String getAndReset() {
        String currentState = tenantId;
        tenantId = null;
        return currentState == null ? "" : currentState;
    }
}
