package io.quarkus.it.keycloak;

import io.quarkus.arc.Unremovable;
import io.quarkus.oidc.BearerTokenAuthentication;
import io.quarkus.oidc.common.OidcRequestFilter;
import jakarta.enterprise.context.ApplicationScoped;

@BearerTokenAuthentication
@ApplicationScoped
@Unremovable
public class CustomOidcRequestFilter implements OidcRequestFilter {

    @Override
    public void filter(OidcRequestContext requestContext) {
        requestContext.request().putHeader("custom-header-name", "custom-header-value");
    }
}
