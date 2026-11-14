package io.quarkus.oidc.test;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.arc.Unremovable;
import io.quarkus.oidc.OidcRedirectFilter;
import io.quarkus.oidc.Redirect;

@ApplicationScoped
@Unremovable
@Redirect(Redirect.Location.APPLICATION_LOGIN_PAGE)
public class ApplicationLoginPageRedirectFilter implements OidcRedirectFilter {

    @Override
    public void filter(OidcRedirectContext context) {
        context.additionalQueryParams().add("login-page-filter", "applied");
    }
}
