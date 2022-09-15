package io.quarkus.oidc.runtime;

import java.util.Collections;
import java.util.Set;
import java.util.function.Function;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.oidc.OIDCException;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.OidcTenantConfig.ApplicationType;
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
    private static HttpCredentialTransport OIDC_SERVICE_TRANSPORT = new HttpCredentialTransport(
            HttpCredentialTransport.Type.AUTHORIZATION, OidcConstants.BEARER_SCHEME);
    private static HttpCredentialTransport OIDC_WEB_APP_TRANSPORT = new HttpCredentialTransport(
            HttpCredentialTransport.Type.AUTHORIZATION_CODE, OidcConstants.CODE_FLOW_CODE);

    private final BearerAuthenticationMechanism bearerAuth = new BearerAuthenticationMechanism();
    private final CodeAuthenticationMechanism codeAuth = new CodeAuthenticationMechanism();
    private final DefaultTenantConfigResolver resolver;

    public OidcAuthenticationMechanism(DefaultTenantConfigResolver resolver) {
        this.resolver = resolver;
        this.bearerAuth.init(this, resolver);
        this.codeAuth.init(this, resolver);
    }

    @Override
    public Uni<SecurityIdentity> authenticate(RoutingContext context,
            IdentityProviderManager identityProviderManager) {
        setTenantIdAttribute(context);
        return resolve(context).chain(new Function<>() {
            @Override
            public Uni<? extends SecurityIdentity> apply(OidcTenantConfig oidcConfig) {
                if (!oidcConfig.tenantEnabled) {
                    return Uni.createFrom().nullItem();
                }
                return isWebApp(context, oidcConfig) ? codeAuth.authenticate(context, identityProviderManager, oidcConfig)
                        : bearerAuth.authenticate(context, identityProviderManager, oidcConfig);
            }
        });
    }

    @Override
    public Uni<ChallengeData> getChallenge(RoutingContext context) {
        setTenantIdAttribute(context);
        return resolve(context).chain(new Function<>() {
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
        return resolver.resolveConfig(context).map(new Function<>() {
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
        ApplicationType applicationType = oidcConfig.applicationType.orElse(ApplicationType.SERVICE);
        if (OidcTenantConfig.ApplicationType.HYBRID == applicationType) {
            return context.request().getHeader("Authorization") == null;
        }
        return OidcTenantConfig.ApplicationType.WEB_APP == applicationType;
    }

    @Override
    public Set<Class<? extends AuthenticationRequest>> getCredentialTypes() {
        return Collections.singleton(TokenAuthenticationRequest.class);
    }

    @Override
    public Uni<HttpCredentialTransport> getCredentialTransport(RoutingContext context) {
        setTenantIdAttribute(context);
        return resolve(context).onItem().transform(new Function<OidcTenantConfig, HttpCredentialTransport>() {
            @Override
            public HttpCredentialTransport apply(OidcTenantConfig oidcTenantConfig) {
                if (!oidcTenantConfig.tenantEnabled) {
                    return null;
                }
                return isWebApp(context, oidcTenantConfig) ? OIDC_WEB_APP_TRANSPORT
                        : OIDC_SERVICE_TRANSPORT;
            }
        });
    }

    private static void setTenantIdAttribute(RoutingContext context) {
        for (String cookieName : context.cookieMap().keySet()) {
            if (cookieName.startsWith(OidcUtils.SESSION_COOKIE_NAME)) {
                setTenantIdAttribute(context, OidcUtils.SESSION_COOKIE_NAME, cookieName);
            } else if (cookieName.startsWith(OidcUtils.STATE_COOKIE_NAME)) {
                setTenantIdAttribute(context, OidcUtils.STATE_COOKIE_NAME, cookieName);
            }
        }
    }

    private static void setTenantIdAttribute(RoutingContext context, String cookiePrefix, String cookieName) {
        // It has already been checked the cookieName starts with the cookiePrefix
        if (cookieName.length() == cookiePrefix.length()) {
            context.put(OidcUtils.TENANT_ID_ATTRIBUTE, OidcUtils.DEFAULT_TENANT_ID);
        } else {
            String suffix = cookieName.substring(cookiePrefix.length() + 1);
            // It can be either a tenant_id, or a tenant_id and cookie suffix property, example, q_session_github or q_session_github_test
            int index = suffix.indexOf("_");
            String tenantId = index == -1 ? suffix : suffix.substring(0, index);
            context.put(OidcUtils.TENANT_ID_ATTRIBUTE, tenantId);
        }
    }

    @Override
    public int getPriority() {
        return HttpAuthenticationMechanism.DEFAULT_PRIORITY + 1;
    }
}
