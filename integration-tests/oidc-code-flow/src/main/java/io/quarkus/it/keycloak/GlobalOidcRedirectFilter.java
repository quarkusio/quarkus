package io.quarkus.it.keycloak;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.arc.Unremovable;
import io.quarkus.oidc.OidcRedirectFilter;

@ApplicationScoped
@Unremovable
public class GlobalOidcRedirectFilter implements OidcRedirectFilter {

    @Override
    public void filter(OidcRedirectContext context) {
        if (context.redirectUri().contains("/session-expired-page")) {
            context.additionalQueryParams().add("redirect-filtered", "true,");
        }
    }

}
