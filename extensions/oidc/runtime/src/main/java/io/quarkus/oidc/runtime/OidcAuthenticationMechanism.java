package io.quarkus.oidc.runtime;

import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.quarkus.oidc.OIDCException;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class OidcAuthenticationMechanism implements HttpAuthenticationMechanism {

    @Inject
    DefaultTenantConfigResolver resolver;
    private BearerAuthenticationMechanism bearerAuth = new BearerAuthenticationMechanism();
    private CodeAuthenticationMechanism codeAuth = new CodeAuthenticationMechanism();

    @Override
    public CompletionStage<SecurityIdentity> authenticate(RoutingContext context,
            IdentityProviderManager identityProviderManager) {
        return isWebApp(context) ? codeAuth.authenticate(context, identityProviderManager, resolver)
                : bearerAuth.authenticate(context, identityProviderManager, resolver);
    }

    @Override
    public CompletionStage<ChallengeData> getChallenge(RoutingContext context) {
        return isWebApp(context) ? codeAuth.getChallenge(context, resolver)
                : bearerAuth.getChallenge(context, resolver);
    }

    private boolean isWebApp(RoutingContext context) {
        TenantConfigContext tenantContext = resolver.resolve(context, false);
        if (tenantContext == null) {
            throw new OIDCException("Tenant configuration context has not been resolved");
        }
        return OidcTenantConfig.ApplicationType.WEB_APP == tenantContext.oidcConfig.applicationType;
    }

}
