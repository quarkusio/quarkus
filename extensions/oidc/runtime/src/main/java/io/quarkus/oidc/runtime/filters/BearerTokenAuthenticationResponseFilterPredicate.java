package io.quarkus.oidc.runtime.filters;

import static io.quarkus.oidc.common.runtime.OidcConstants.BEARER_SCHEME;
import static io.quarkus.oidc.runtime.OidcUtils.OIDC_AUTH_MECHANISM;

import io.quarkus.oidc.common.OidcResponseFilter;
import io.quarkus.oidc.common.runtime.OidcFilterStorage;

public final class BearerTokenAuthenticationResponseFilterPredicate implements OidcFilterStorage.OidcResponseContextPredicate {

    public BearerTokenAuthenticationResponseFilterPredicate() {
    }

    @Override
    public boolean test(OidcResponseFilter.OidcResponseContext oidcResponseContext) {
        var contextProperties = oidcResponseContext.requestProperties();
        if (contextProperties == null) {
            return false;
        }
        return BEARER_SCHEME.equals(contextProperties.getString(OIDC_AUTH_MECHANISM));
    }
}
