package io.quarkus.oidc.runtime.filters;

import static io.quarkus.oidc.common.runtime.OidcConstants.CODE_FLOW_CODE;
import static io.quarkus.oidc.runtime.OidcUtils.OIDC_AUTH_MECHANISM;

import io.quarkus.oidc.common.OidcRequestFilter;
import io.quarkus.oidc.common.runtime.OidcFilterStorage;

public final class AuthorizationCodeFlowRequestFilterPredicate implements OidcFilterStorage.OidcRequestContextPredicate {

    public AuthorizationCodeFlowRequestFilterPredicate() {
    }

    @Override
    public boolean test(OidcRequestFilter.OidcRequestContext oidcRequestContext) {
        var contextProperties = oidcRequestContext.contextProperties();
        if (contextProperties == null) {
            return false;
        }
        return CODE_FLOW_CODE.equals(contextProperties.getString(OIDC_AUTH_MECHANISM));
    }
}
