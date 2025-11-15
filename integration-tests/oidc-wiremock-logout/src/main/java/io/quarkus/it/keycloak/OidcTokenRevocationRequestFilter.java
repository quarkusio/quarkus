package io.quarkus.it.keycloak;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.arc.Unremovable;
import io.quarkus.oidc.common.OidcEndpoint;
import io.quarkus.oidc.common.OidcEndpoint.Type;
import io.quarkus.oidc.common.OidcRequestFilter;

@ApplicationScoped
@Unremovable
@OidcEndpoint(value = { Type.TOKEN_REVOCATION })
public class OidcTokenRevocationRequestFilter implements OidcRequestFilter {

    @Override
    public void filter(OidcRequestContext rc) {
        String body = rc.requestBody().toString();
        if (body.contains("token_type_hint=access_token")) {
            rc.request().putHeader("token-revocation-filter", "access-token");
        } else if (body.contains("token_type_hint=refresh_token")) {
            rc.request().putHeader("token-revocation-filter", "refresh-token");
        }

    }
}
