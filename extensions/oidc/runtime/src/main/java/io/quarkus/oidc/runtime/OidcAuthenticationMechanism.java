package io.quarkus.oidc.runtime;

import java.util.Collections;
import java.util.Set;
import java.util.function.Function;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.quarkus.oidc.OIDCException;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.common.runtime.OidcConstants;
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

    @PostConstruct
    public void init() {
        bearerAuth.setResolver(resolver);
        codeAuth.setResolver(resolver);
    }

    @Override
    public Uni<SecurityIdentity> authenticate(RoutingContext context,
            IdentityProviderManager identityProviderManager) {
        return resolve(context).chain(new Function<OidcTenantConfig, Uni<? extends SecurityIdentity>>() {
            @Override
            public Uni<? extends SecurityIdentity> apply(OidcTenantConfig oidcConfig) {
                if (!oidcConfig.tenantEnabled) {
                    return Uni.createFrom().nullItem();
                }
                return isWebApp(context, oidcConfig) ? codeAuth.authenticate(context, identityProviderManager)
                        : bearerAuth.authenticate(context, identityProviderManager);
            }
        });
    }

    @Override
    public Uni<ChallengeData> getChallenge(RoutingContext context) {
        return resolve(context).chain(new Function<OidcTenantConfig, Uni<? extends ChallengeData>>() {
            @Override
            public Uni<? extends ChallengeData> apply(OidcTenantConfig oidcTenantConfig) {
                if (!oidcTenantConfig.tenantEnabled) {
                    return Uni.createFrom().nullItem();
                }
                return isWebApp(context, oidcTenantConfig) ? codeAuth.getChallenge(context)
                        : bearerAuth.getChallenge(context);
            }
        });
    }

    private Uni<OidcTenantConfig> resolve(RoutingContext context) {
        return resolver.resolveConfig(context).map(new Function<OidcTenantConfig, OidcTenantConfig>() {
            @Override
            public OidcTenantConfig apply(OidcTenantConfig oidcTenantConfig) {
                if (oidcTenantConfig == null) {
                    throw new OIDCException("Tenant configuration has not been resolved");
                }
                return oidcTenantConfig;
            };
        });
    }

    private boolean isWebApp(RoutingContext context, OidcTenantConfig oidcConfig) {
        if (OidcTenantConfig.ApplicationType.HYBRID == oidcConfig.applicationType) {
            return context.request().getHeader("Authorization") == null;
        }
        return OidcTenantConfig.ApplicationType.WEB_APP == oidcConfig.applicationType;
    }

    @Override
    public Set<Class<? extends AuthenticationRequest>> getCredentialTypes() {
        return Collections.singleton(TokenAuthenticationRequest.class);
    }

    @Override
    public HttpCredentialTransport getCredentialTransport() {
        //not 100% correct, but enough for now
        //if OIDC is present we don't really want another bearer mechanism
        return new HttpCredentialTransport(HttpCredentialTransport.Type.AUTHORIZATION, OidcConstants.BEARER_SCHEME);
    }
}
