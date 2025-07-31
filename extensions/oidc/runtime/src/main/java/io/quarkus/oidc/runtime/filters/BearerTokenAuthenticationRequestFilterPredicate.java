package io.quarkus.oidc.runtime.filters;

import static io.quarkus.oidc.common.runtime.OidcConstants.BEARER_SCHEME;
import static io.quarkus.oidc.runtime.OidcUtils.OIDC_AUTH_MECHANISM;

import io.quarkus.oidc.common.OidcRequestFilter;
import io.quarkus.oidc.common.runtime.OidcFilterStorage;

public final class BearerTokenAuthenticationRequestFilterPredicate implements OidcFilterStorage.OidcRequestContextPredicate {

    public BearerTokenAuthenticationRequestFilterPredicate() {
    }

    @Override
    public boolean test(OidcRequestFilter.OidcRequestContext oidcRequestContext) {
        var contextProperties = oidcRequestContext.contextProperties();
        if (contextProperties == null) {
            return false;
        }
        return BEARER_SCHEME.equals(contextProperties.getString(OIDC_AUTH_MECHANISM));
    }
}
