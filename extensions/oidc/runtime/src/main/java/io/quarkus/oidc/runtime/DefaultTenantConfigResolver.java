package io.quarkus.oidc.runtime;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import io.quarkus.oidc.JavaScriptRequestChecker;
import io.quarkus.oidc.OIDCException;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.SecurityEvent;
import io.quarkus.oidc.TenantConfigResolver;
import io.quarkus.oidc.TenantResolver;
import io.quarkus.oidc.TokenIntrospectionCache;
import io.quarkus.oidc.TokenStateManager;
import io.quarkus.oidc.UserInfo;
import io.quarkus.oidc.UserInfoCache;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.spi.runtime.BlockingSecurityExecutor;
import io.quarkus.security.spi.runtime.SecurityEventHelper;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class DefaultTenantConfigResolver {

    private static final Logger LOG = Logger.getLogger(DefaultTenantConfigResolver.class);
    private static final String CURRENT_STATIC_TENANT_ID = "static.tenant.id";
    private static final String CURRENT_STATIC_TENANT_ID_NULL = "static.tenant.id.null";
    private static final String CURRENT_DYNAMIC_TENANT_CONFIG = "dynamic.tenant.config";

    private DefaultStaticTenantResolver defaultStaticTenantResolver = new DefaultStaticTenantResolver();

    @Inject
    Instance<TenantResolver> tenantResolver;

    @Inject
    Instance<TenantConfigResolver> tenantConfigResolver;

    @Inject
    Instance<JavaScriptRequestChecker> javaScriptRequestChecker;

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

    private final BlockingTaskRunner<OidcTenantConfig> blockingRequestContext;

    private final boolean securityEventObserved;

    private ConcurrentHashMap<String, BackChannelLogoutTokenCache> backChannelLogoutTokens = new ConcurrentHashMap<>();

    public DefaultTenantConfigResolver(BlockingSecurityExecutor blockingExecutor, BeanManager beanManager,
            @ConfigProperty(name = "quarkus.security.events.enabled") boolean securityEventsEnabled) {
        this.blockingRequestContext = new BlockingTaskRunner<OidcTenantConfig>(blockingExecutor);
        this.securityEventObserved = SecurityEventHelper.isEventObserved(new SecurityEvent(null, (SecurityIdentity) null),
                beanManager, securityEventsEnabled);
    }

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
        if (javaScriptRequestChecker.isAmbiguous()) {
            throw new IllegalStateException("Multiple " + JavaScriptRequestChecker.class + " beans registered");
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

    Uni<TenantConfigContext> resolveContext(String tenantId) {
        return initializeTenantIfContextNotReady(getStaticTenantContext(tenantId));
    }

    Uni<TenantConfigContext> resolveContext(RoutingContext context) {
        return getDynamicTenantContext(context).onItem().ifNull().switchTo(new Supplier<Uni<? extends TenantConfigContext>>() {
            @Override
            public Uni<? extends TenantConfigContext> get() {
                return initializeTenantIfContextNotReady(getStaticTenantContext(context));
            }
        });
    }

    private Uni<TenantConfigContext> initializeTenantIfContextNotReady(TenantConfigContext tenantContext) {
        if (tenantContext != null && !tenantContext.ready) {

            // check if the connection has already been created
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

    private TenantConfigContext getStaticTenantContext(RoutingContext context) {

        String tenantId = context.get(CURRENT_STATIC_TENANT_ID);

        if (tenantId == null && context.get(CURRENT_STATIC_TENANT_ID_NULL) == null) {
            if (tenantResolver.isResolvable()) {
                tenantId = tenantResolver.get().resolve(context);
            }

            if (tenantId == null && tenantConfigBean.getStaticTenantsConfig().size() > 0) {
                tenantId = defaultStaticTenantResolver.resolve(context);
            }

            if (tenantId == null) {
                tenantId = context.get(OidcUtils.TENANT_ID_ATTRIBUTE);
            }
        }

        if (tenantId != null) {
            context.put(CURRENT_STATIC_TENANT_ID, tenantId);
        } else {
            context.put(CURRENT_STATIC_TENANT_ID_NULL, true);
        }

        return getStaticTenantContext(tenantId);
    }

    private TenantConfigContext getStaticTenantContext(String tenantId) {
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
                oidcConfig = tenantConfigResolver.get().resolve(context, blockingRequestContext);
                if (oidcConfig == null) {
                    //shouldn't happen, but guard against it anyway
                    oidcConfig = Uni.createFrom().nullItem();
                }
                oidcConfig = oidcConfig.memoize().indefinitely();
                if (oidcConfig == null) {
                    //shouldn't happen, but guard against it anyway
                    oidcConfig = Uni.createFrom().nullItem();
                } else {
                    oidcConfig = oidcConfig.onItem().transform(cfg -> OidcUtils.resolveProviderConfig(cfg));
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

    public Map<String, BackChannelLogoutTokenCache> getBackChannelLogoutTokens() {
        return backChannelLogoutTokens;
    }

    public TenantConfigBean getTenantConfigBean() {
        return tenantConfigBean;
    }

    public JavaScriptRequestChecker getJavaScriptRequestChecker() {
        return javaScriptRequestChecker.isResolvable() ? javaScriptRequestChecker.get() : null;
    }

    private class DefaultStaticTenantResolver implements TenantResolver {

        @Override
        public String resolve(RoutingContext context) {
            String tenantId = context.get(OidcUtils.TENANT_ID_ATTRIBUTE);
            if (tenantId != null) {
                return tenantId;
            }
            String[] pathSegments = context.request().path().split("/");
            if (pathSegments.length > 0) {
                String lastPathSegment = pathSegments[pathSegments.length - 1];
                if (tenantConfigBean.getStaticTenantsConfig().containsKey(lastPathSegment)) {
                    return lastPathSegment;
                }
            }
            return null;
        }

    }

}
