package io.quarkus.oidc.runtime;

import java.util.Collections;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.quarkus.oidc.OIDCException;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.AuthenticationRequest;
import io.quarkus.security.identity.request.TokenAuthenticationRequest;
import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism;
import io.quarkus.vertx.http.runtime.security.HttpCredentialTransport;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class OidcAuthenticationMechanism implements HttpAuthenticationMechanism {

    @Inject
    DefaultTenantConfigResolver resolver;
    private BearerAuthenticationMechanism bearerAuth = new BearerAuthenticationMechanism();
    private CodeAuthenticationMechanism codeAuth = new CodeAuthenticationMechanism();

    @Override
    public Uni<SecurityIdentity> authenticate(RoutingContext context,
            IdentityProviderManager identityProviderManager) {
        TenantConfigContext tenantContext = resolve(context);
        if (tenantContext.oidcConfig.tenantEnabled == false) {
            return Uni.createFrom().nullItem();
        }
        return isWebApp(tenantContext) ? codeAuth.authenticate(context, identityProviderManager, resolver)
                : bearerAuth.authenticate(context, identityProviderManager, resolver);
    }

    @Override
    public Uni<ChallengeData> getChallenge(RoutingContext context) {
        TenantConfigContext tenantContext = resolve(context);
        if (tenantContext.oidcConfig.tenantEnabled == false) {
            return Uni.createFrom().nullItem();
        }
        return isWebApp(tenantContext) ? codeAuth.getChallenge(context, resolver)
                : bearerAuth.getChallenge(context, resolver);
    }

    private TenantConfigContext resolve(RoutingContext context) {
        TenantConfigContext tenantContext = resolver.resolve(context, false);
        if (tenantContext == null) {
            throw new OIDCException("Tenant configuration context has not been resolved");
        }
        return tenantContext;
    }

    private boolean isWebApp(TenantConfigContext tenantContext) {
        return OidcTenantConfig.ApplicationType.WEB_APP == tenantContext.oidcConfig.applicationType;
    }

    @Override
    public Set<Class<? extends AuthenticationRequest>> getCredentialTypes() {
        return Collections.singleton(TokenAuthenticationRequest.class);
    }

    @Override
    public HttpCredentialTransport getCredentialTransport() {
        //not 100% correct, but enough for now
        //if OIDC is present we don't really want another bearer mechanism
        return new HttpCredentialTransport(HttpCredentialTransport.Type.AUTHORIZATION, "bearer");
    }
}
