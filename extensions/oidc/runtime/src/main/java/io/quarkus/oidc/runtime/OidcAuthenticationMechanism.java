package io.quarkus.oidc.runtime;

import static io.quarkus.oidc.runtime.OidcAuthenticationPolicy.AUTHENTICATION_POLICY_KEY;

import java.util.Collections;
import java.util.Set;
import java.util.function.Function;

import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.logging.Logger;

import io.quarkus.oidc.OIDCException;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.common.runtime.OidcConstants;
import io.quarkus.oidc.runtime.OidcTenantConfig.ApplicationType;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.AuthenticationRequest;
import io.quarkus.security.identity.request.TokenAuthenticationRequest;
import io.quarkus.security.spi.runtime.BlockingSecurityExecutor;
import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism;
import io.quarkus.vertx.http.runtime.security.HttpCredentialTransport;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class OidcAuthenticationMechanism implements HttpAuthenticationMechanism {
    private static final Logger LOG = Logger.getLogger(OidcAuthenticationMechanism.class);

    private static final HttpCredentialTransport OIDC_WEB_APP_TRANSPORT = new HttpCredentialTransport(
            HttpCredentialTransport.Type.AUTHORIZATION_CODE, OidcConstants.CODE_FLOW_CODE);

    private final BearerAuthenticationMechanism bearerAuth = new BearerAuthenticationMechanism();
    private final CodeAuthenticationMechanism codeAuth;
    private final DefaultTenantConfigResolver resolver;

    public OidcAuthenticationMechanism(DefaultTenantConfigResolver resolver, BlockingSecurityExecutor blockingExecutor) {
        this.resolver = resolver;
        this.codeAuth = new CodeAuthenticationMechanism(blockingExecutor);
        this.bearerAuth.init(this, resolver);
        this.codeAuth.init(this, resolver);
    }

    @Override
    public Uni<SecurityIdentity> authenticate(RoutingContext context, IdentityProviderManager identityProviderManager) {
        return resolve(context).chain(new Function<>() {
            @Override
            public Uni<? extends SecurityIdentity> apply(OidcTenantConfig oidcConfig) {
                if (!oidcConfig.tenantEnabled()) {
                    return Uni.createFrom().nullItem();
                }
                OidcAuthenticationPolicy authenticationPolicy = context.get(AUTHENTICATION_POLICY_KEY);
                if (authenticationPolicy != null) {
                    return authenticate(oidcConfig, context, identityProviderManager).invoke(authenticationPolicy);
                }
                return authenticate(oidcConfig, context, identityProviderManager);
            }
        });
    }

    private Uni<SecurityIdentity> authenticate(OidcTenantConfig oidcConfig, RoutingContext context,
            IdentityProviderManager identityProviderManager) {
        return isWebApp(context, oidcConfig) ? codeAuth.authenticate(context, identityProviderManager, oidcConfig)
                : bearerAuth.authenticate(context, identityProviderManager, oidcConfig);
    }

    @Override
    public Uni<ChallengeData> getChallenge(RoutingContext context) {
        return resolve(context).chain(new Function<>() {
            @Override
            public Uni<? extends ChallengeData> apply(OidcTenantConfig oidcTenantConfig) {
                if (!oidcTenantConfig.tenantEnabled()) {
                    return Uni.createFrom().nullItem();
                }
                return isWebApp(context, oidcTenantConfig) ? codeAuth.getChallenge(context)
                        : bearerAuth.getChallenge(context);
            }
        });
    }

    private Uni<OidcTenantConfig> resolve(RoutingContext context) {
        OidcTenantConfig resolvedConfig = context.get(OidcTenantConfig.class.getName());
        if (resolvedConfig != null) {
            return Uni.createFrom().item(resolvedConfig);
        }

        setTenantIdAttribute(context);

        return resolver.resolveConfig(context).map(new Function<>() {
            @Override
            public OidcTenantConfig apply(OidcTenantConfig oidcTenantConfig) {
                if (oidcTenantConfig == null) {
                    throw new OIDCException("Tenant configuration has not been resolved");
                }
                final String tenantId = oidcTenantConfig.tenantId().orElse(OidcUtils.DEFAULT_TENANT_ID);
                LOG.debugf("Resolved OIDC tenant id: %s", tenantId);
                context.put(OidcTenantConfig.class.getName(), oidcTenantConfig);
                if (context.get(OidcUtils.TENANT_ID_ATTRIBUTE) == null) {
                    context.put(OidcUtils.TENANT_ID_ATTRIBUTE, tenantId);
                }
                return oidcTenantConfig;
            };
        });
    }

    private boolean isWebApp(RoutingContext context, OidcTenantConfig oidcConfig) {
        ApplicationType applicationType = oidcConfig.applicationType().orElse(ApplicationType.SERVICE);
        if (ApplicationType.HYBRID == applicationType) {
            return context.request().getHeader("Authorization") == null;
        }
        return ApplicationType.WEB_APP == applicationType;
    }

    @Override
    public Set<Class<? extends AuthenticationRequest>> getCredentialTypes() {
        return Collections.singleton(TokenAuthenticationRequest.class);
    }

    @Override
    public Uni<HttpCredentialTransport> getCredentialTransport(RoutingContext context) {
        return resolve(context).onItem().transform(new Function<OidcTenantConfig, HttpCredentialTransport>() {
            @Override
            public HttpCredentialTransport apply(OidcTenantConfig oidcTenantConfig) {
                if (!oidcTenantConfig.tenantEnabled()) {
                    return null;
                }
                return isWebApp(context, oidcTenantConfig) ? OIDC_WEB_APP_TRANSPORT
                        : new HttpCredentialTransport(
                                HttpCredentialTransport.Type.AUTHORIZATION, oidcTenantConfig.token().authorizationScheme());
            }
        });
    }

    private static void setTenantIdAttribute(RoutingContext context) {
        if (context.get(OidcUtils.TENANT_ID_ATTRIBUTE) == null) {
            for (var cookie : context.request().cookies()) {
                String cookieName = cookie.getName();
                if (OidcUtils.isSessionCookie(cookieName)) {
                    setTenantIdAttribute(context, OidcUtils.SESSION_COOKIE_NAME, cookieName, true);
                    break;
                } else if (cookieName.startsWith(OidcUtils.STATE_COOKIE_NAME)) {
                    setTenantIdAttribute(context, OidcUtils.STATE_COOKIE_NAME, cookieName, false);
                    break;
                }
            }
        }
    }

    private static void setTenantIdAttribute(RoutingContext context, String cookiePrefix, String cookieName,
            boolean sessionCookie) {
        String tenantId = OidcUtils.getTenantIdFromCookie(cookiePrefix, cookieName, sessionCookie);

        context.put(OidcUtils.TENANT_ID_ATTRIBUTE, tenantId);
        context.put(sessionCookie ? OidcUtils.TENANT_ID_SET_BY_SESSION_COOKIE : OidcUtils.TENANT_ID_SET_BY_STATE_COOKIE,
                tenantId);
        LOG.debugf("%s cookie set a '%s' tenant id on the %s request path", cookieName, tenantId, context.request().path());
    }

    @Override
    public int getPriority() {
        return HttpAuthenticationMechanism.DEFAULT_PRIORITY + 1;
    }
}
