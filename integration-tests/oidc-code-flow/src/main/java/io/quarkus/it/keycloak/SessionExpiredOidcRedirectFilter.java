package io.quarkus.it.keycloak;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.jwt.Claims;

import io.quarkus.arc.Unremovable;
import io.quarkus.oidc.AuthorizationCodeTokens;
import io.quarkus.oidc.OidcRedirectFilter;
import io.quarkus.oidc.Redirect;
import io.quarkus.oidc.Redirect.Location;
import io.quarkus.oidc.TenantFeature;
import io.quarkus.oidc.common.runtime.OidcCommonUtils;
import io.quarkus.oidc.runtime.OidcUtils;
import io.smallrye.jwt.build.Jwt;

@ApplicationScoped
@Unremovable
@TenantFeature("tenant-refresh")
@Redirect(Location.SESSION_EXPIRED_PAGE)
public class SessionExpiredOidcRedirectFilter implements OidcRedirectFilter {

    @Override
    public void filter(OidcRedirectContext context) {

        if (!"tenant-refresh".equals(context.oidcTenantConfig().tenantId.get())) {
            throw new RuntimeException("Invalid tenant id");
        }

        if (!context.redirectUri().contains("/session-expired-page")) {
            throw new RuntimeException("Invalid redirect URI");
        }

        AuthorizationCodeTokens tokens = context.routingContext().get(AuthorizationCodeTokens.class.getName());
        String userName = OidcCommonUtils.decodeJwtContent(tokens.getIdToken()).getString(Claims.preferred_username.name());
        String jwe = Jwt.preferredUserName(userName).jwe()
                .encryptWithSecret(context.oidcTenantConfig().credentials.secret.get());
        OidcUtils.createCookie(context.routingContext(), context.oidcTenantConfig(), "session_expired",
                jwe + "|" + context.oidcTenantConfig().tenantId.get(), 10);

        context.additionalQueryParams().add("session-expired", "true");
    }

}
