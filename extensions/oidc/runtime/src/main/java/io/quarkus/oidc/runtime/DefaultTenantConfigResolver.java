package io.quarkus.oidc.runtime;

import java.util.function.Function;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import io.quarkus.oidc.OIDCException;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.SecurityEvent;
import io.quarkus.oidc.TenantConfigResolver;
import io.quarkus.oidc.TenantResolver;
import io.quarkus.oidc.TokenIntrospectionCache;
import io.quarkus.oidc.TokenStateManager;
import io.quarkus.oidc.UserInfo;
import io.quarkus.oidc.UserInfoCache;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class DefaultTenantConfigResolver {

    private static final Logger LOG = Logger.getLogger(DefaultTenantConfigResolver.class);
    private static final String CURRENT_STATIC_TENANT_ID = "static.tenant.id";
    private static final String CURRENT_STATIC_TENANT_ID_NULL = "static.tenant.id.null";
    private static final String CURRENT_DYNAMIC_TENANT_CONFIG = "dynamic.tenant.config";

    @Inject
    Instance<TenantResolver> tenantResolver;

    @Inject
    Instance<TenantConfigResolver> tenantConfigResolver;

    @Inject
    TenantConfigBean tenantConfigBean;

    @Inject
    Instance<TokenStateManager> tokenStateManager;

    @Inject
    Instance<TokenIntrospectionCache> tokenIntrospectionCache;

    @Inject
    Instance<UserInfoCache> userInfoCache;

    @Inject
    Event<SecurityEvent> securityEvent;

    @Inject
    @ConfigProperty(name = "quarkus.http.proxy.enable-forwarded-prefix")
    boolean enableHttpForwardedPrefix;

    private final BlockingTaskRunner<OidcTenantConfig> blockingRequestContext = new BlockingTaskRunner<OidcTenantConfig>();

    private volatile boolean securityEventObserved;

    @PostConstruct
    public void verifyResolvers() {
        if (tenantConfigResolver.isResolvable() && tenantConfigResolver.isAmbiguous()) {
            throw new IllegalStateException("Multiple " + TenantConfigResolver.class + " beans registered");
        }
        if (tenantResolver.isResolvable() && tenantResolver.isAmbiguous()) {
            throw new IllegalStateException("Multiple " + TenantResolver.class + " beans registered");
        }
        if (tokenStateManager.isAmbiguous()) {
            throw new IllegalStateException("Multiple " + TokenStateManager.class + " beans registered");
        }
        if (tokenIntrospectionCache.isAmbiguous()) {
            throw new IllegalStateException("Multiple " + TokenIntrospectionCache.class + " beans registered");
        }
        if (userInfoCache.isAmbiguous()) {
            throw new IllegalStateException("Multiple " + UserInfo.class + " beans registered");
        }
    }

    Uni<OidcTenantConfig> resolveConfig(RoutingContext context) {
        return getDynamicTenantConfig(context)
                .map(new Function<OidcTenantConfig, OidcTenantConfig>() {
                    @Override
                    public OidcTenantConfig apply(OidcTenantConfig tenantConfig) {
                        if (tenantConfig == null) {
                            TenantConfigContext tenant = getStaticTenantContext(context);
                            if (tenant != null) {
                                tenantConfig = tenant.oidcConfig;
                            }
                        }
                        return tenantConfig;
                    }
                });
    }

    Uni<TenantConfigContext> resolveContext(RoutingContext context) {
        return getDynamicTenantContext(context).chain(new Function<TenantConfigContext, Uni<? extends TenantConfigContext>>() {
            @Override
            public Uni<? extends TenantConfigContext> apply(TenantConfigContext tenantConfigContext) {
                if (tenantConfigContext != null) {
                    return Uni.createFrom().item(tenantConfigContext);
                }
                TenantConfigContext tenantContext = getStaticTenantContext(context);
                if (tenantContext != null && !tenantContext.ready) {

                    // check if it the connection has already been created
                    TenantConfigContext readyTenantContext = tenantConfigBean.getDynamicTenantsConfig()
                            .get(tenantContext.oidcConfig.tenantId.get());
                    if (readyTenantContext == null) {
                        LOG.debugf("Tenant '%s' is not initialized yet, trying to create OIDC connection now",
                                tenantContext.oidcConfig.tenantId.get());
                        return tenantConfigBean.getTenantConfigContextFactory().apply(tenantContext.oidcConfig);
                    } else {
                        tenantContext = readyTenantContext;
                    }
                }

                return Uni.createFrom().item(tenantContext);
            }
        });
    }

    private TenantConfigContext getStaticTenantContext(RoutingContext context) {

        String tenantId = null;

        if (tenantResolver.isResolvable()) {
            tenantId = context.get(CURRENT_STATIC_TENANT_ID);
            if (tenantId == null && context.get(CURRENT_STATIC_TENANT_ID_NULL) == null) {
                tenantId = tenantResolver.get().resolve(context);
                if (tenantId != null) {
                    context.put(CURRENT_STATIC_TENANT_ID, tenantId);
                } else {
                    context.put(CURRENT_STATIC_TENANT_ID_NULL, true);
                }
            }
        }

        TenantConfigContext configContext = tenantId != null ? tenantConfigBean.getStaticTenantsConfig().get(tenantId) : null;
        if (configContext == null) {
            if (tenantId != null && !tenantId.isEmpty()) {
                LOG.debugf(
                        "Registered TenantResolver has not provided the configuration for tenant '%s', using the default tenant",
                        tenantId);
            }
            configContext = tenantConfigBean.getDefaultTenant();
        }
        return configContext;
    }

    boolean isSecurityEventObserved() {
        return securityEventObserved;
    }

    void setSecurityEventObserved(boolean securityEventObserved) {
        this.securityEventObserved = securityEventObserved;
    }

    Event<SecurityEvent> getSecurityEvent() {
        return securityEvent;
    }

    TokenStateManager getTokenStateManager() {
        return tokenStateManager.get();
    }

    TokenIntrospectionCache getTokenIntrospectionCache() {
        return tokenIntrospectionCache.isResolvable() ? tokenIntrospectionCache.get() : null;
    }

    UserInfoCache getUserInfoCache() {
        return userInfoCache.isResolvable() ? userInfoCache.get() : null;
    }

    private Uni<OidcTenantConfig> getDynamicTenantConfig(RoutingContext context) {
        if (tenantConfigResolver.isResolvable()) {
            Uni<OidcTenantConfig> oidcConfig = context.get(CURRENT_DYNAMIC_TENANT_CONFIG);
            if (oidcConfig == null) {
                oidcConfig = tenantConfigResolver.get().resolve(context, blockingRequestContext).memoize().indefinitely();
                if (oidcConfig == null) {
                    //shouldn't happen, but guard against it anyway
                    oidcConfig = Uni.createFrom().nullItem();
                }
                context.put(CURRENT_DYNAMIC_TENANT_CONFIG, oidcConfig);
            }
            return oidcConfig;
        }
        return Uni.createFrom().nullItem();
    }

    private Uni<TenantConfigContext> getDynamicTenantContext(RoutingContext context) {

        return getDynamicTenantConfig(context).chain(new Function<OidcTenantConfig, Uni<? extends TenantConfigContext>>() {
            @Override
            public Uni<? extends TenantConfigContext> apply(OidcTenantConfig tenantConfig) {
                if (tenantConfig != null) {
                    String tenantId = tenantConfig.getTenantId()
                            .orElseThrow(() -> new OIDCException("Tenant configuration must have tenant id"));
                    TenantConfigContext tenantContext = tenantConfigBean.getDynamicTenantsConfig().get(tenantId);
                    if (tenantContext == null) {
                        return tenantConfigBean.getTenantConfigContextFactory().apply(tenantConfig);
                    } else {
                        return Uni.createFrom().item(tenantContext);
                    }
                }
                return Uni.createFrom().nullItem();
            }
        });
    }

    boolean isEnableHttpForwardedPrefix() {
        return enableHttpForwardedPrefix;
    }

}
