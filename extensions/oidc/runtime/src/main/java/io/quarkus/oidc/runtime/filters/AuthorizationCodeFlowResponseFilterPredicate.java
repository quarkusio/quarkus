package io.quarkus.oidc.runtime.filters;

import static io.quarkus.oidc.common.runtime.OidcConstants.CODE_FLOW_CODE;
import static io.quarkus.oidc.runtime.OidcUtils.OIDC_AUTH_MECHANISM;

import io.quarkus.oidc.common.OidcResponseFilter;
import io.quarkus.oidc.common.runtime.OidcFilterStorage;

public final class AuthorizationCodeFlowResponseFilterPredicate implements OidcFilterStorage.OidcResponseContextPredicate {

    public AuthorizationCodeFlowResponseFilterPredicate() {
    }

    @Override
    public boolean test(OidcResponseFilter.OidcResponseContext oidcResponseContext) {
        var contextProperties = oidcResponseContext.requestProperties();
        if (contextProperties == null) {
            return false;
        }
        return CODE_FLOW_CODE.equals(contextProperties.getString(OIDC_AUTH_MECHANISM));
    }
}
