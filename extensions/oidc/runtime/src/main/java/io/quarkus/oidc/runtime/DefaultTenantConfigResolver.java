package io.quarkus.oidc.runtime;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

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
import io.quarkus.security.AuthenticationFailedException;
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
    private static final String REPLACE_TENANT_CONFIG_CONTEXT = "replace-tenant-configuration-context";
    private static final String REMOVE_SESSION_COOKIE = "remove-session-cookie";
    private final ConcurrentHashMap<String, BackChannelLogoutTokenCache> backChannelLogoutTokens = new ConcurrentHashMap<>();
    private final BlockingTaskRunner<OidcTenantConfig> blockingRequestContext;
    private final boolean securityEventObserved;
    private final TenantConfigBean tenantConfigBean;
    private final boolean annotationBasedTenantResolutionEnabled;
    private final String rootPath;
    private final StaticTenantResolver staticTenantResolver;

    @Inject
    Instance<TenantConfigResolver> tenantConfigResolver;

    @Inject
    Instance<JavaScriptRequestChecker> javaScriptRequestChecker;

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

    DefaultTenantConfigResolver(BlockingSecurityExecutor blockingExecutor, BeanManager beanManager,
            Instance<TenantResolver> tenantResolverInstance,
            @ConfigProperty(name = "quarkus.oidc.resolve-tenants-with-issuer") boolean resolveTenantsWithIssuer,
            @ConfigProperty(name = "quarkus.security.events.enabled") boolean securityEventsEnabled,
            @ConfigProperty(name = "quarkus.http.root-path") String rootPath, TenantConfigBean tenantConfigBean) {
        this.blockingRequestContext = new BlockingTaskRunner<OidcTenantConfig>(blockingExecutor);
        this.securityEventObserved = SecurityEventHelper.isEventObserved(new SecurityEvent(null, (SecurityIdentity) null),
                beanManager, securityEventsEnabled);
        this.tenantConfigBean = tenantConfigBean;
        this.annotationBasedTenantResolutionEnabled = Boolean.getBoolean(OidcUtils.ANNOTATION_BASED_TENANT_RESOLUTION_ENABLED);
        this.rootPath = rootPath;
        this.staticTenantResolver = new StaticTenantResolver(tenantConfigBean, rootPath, resolveTenantsWithIssuer,
                tenantResolverInstance);
    }

    @PostConstruct
    public void verifyResolvers() {
        if (tenantConfigResolver.isResolvable() && tenantConfigResolver.isAmbiguous()) {
            throw new IllegalStateException("Multiple " + TenantConfigResolver.class + " beans registered");
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
                .flatMap(new Function<OidcTenantConfig, Uni<? extends OidcTenantConfig>>() {
                    @Override
                    public Uni<OidcTenantConfig> apply(OidcTenantConfig oidcTenantConfig) {
                        if (oidcTenantConfig != null) {
                            return Uni.createFrom().item(oidcTenantConfig);
                        }
                        final String tenantId = context.get(OidcUtils.TENANT_ID_ATTRIBUTE);

                        if (tenantId != null && !isTenantSetByAnnotation(context, tenantId)) {
                            TenantConfigContext tenantContext = tenantConfigBean.getDynamicTenant(tenantId);
                            if (tenantContext != null) {
                                return Uni.createFrom().item(tenantContext.getOidcTenantConfig());
                            }
                        }

                        return getStaticTenantContext(context)
                                .onItem().ifNotNull().transform(TenantConfigContext::getOidcTenantConfig);
                    }
                });
    }

    Uni<TenantConfigContext> resolveContext(String tenantId) {
        return initializeStaticTenantIfContextNotReady(getStaticTenantContext(tenantId));
    }

    Uni<TenantConfigContext> resolveContext(RoutingContext context) {
        return getDynamicTenantContext(context)
                .flatMap(new Function<TenantConfigContext, Uni<? extends TenantConfigContext>>() {
                    @Override
                    public Uni<? extends TenantConfigContext> apply(TenantConfigContext tenantConfigContext) {
                        if (tenantConfigContext != null) {
                            return Uni.createFrom().item(tenantConfigContext);
                        }
                        return getStaticTenantContext(context)
                                .flatMap(DefaultTenantConfigResolver.this::initializeStaticTenantIfContextNotReady);
                    }
                });
    }

    private Uni<TenantConfigContext> initializeStaticTenantIfContextNotReady(TenantConfigContext tenantContext) {
        if (tenantContext != null && !tenantContext.ready()) {
            return tenantContext.initialize();
        }

        return Uni.createFrom().item(tenantContext);
    }

    private Uni<TenantConfigContext> getStaticTenantContext(RoutingContext context) {
        String tenantId = context.get(CURRENT_STATIC_TENANT_ID);
        if (tenantId != null) {
            return Uni.createFrom().item(getStaticTenantContext(tenantId));
        }

        if (context.get(CURRENT_STATIC_TENANT_ID_NULL) == null) {
            return resolveStaticTenantId(context)
                    .map(new Function<String, TenantConfigContext>() {
                        @Override
                        public TenantConfigContext apply(String tenantId) {
                            if (tenantId != null) {
                                context.put(CURRENT_STATIC_TENANT_ID, tenantId);
                            } else {
                                context.put(CURRENT_STATIC_TENANT_ID_NULL, true);
                            }
                            return getStaticTenantContext(tenantId);
                        }
                    });
        }

        return Uni.createFrom().item(getStaticTenantContext((String) null));
    }

    private Uni<String> resolveStaticTenantId(RoutingContext context) {
        String tenantId = context.get(OidcUtils.TENANT_ID_ATTRIBUTE);
        if (isTenantSetByAnnotation(context, tenantId)) {
            return Uni.createFrom().item(tenantId);
        }

        return staticTenantResolver.resolve(context).map(new Function<String, String>() {
            @Override
            public String apply(String tenantId) {
                if (tenantId == null) {
                    return context.get(OidcUtils.TENANT_ID_ATTRIBUTE);
                }
                return tenantId;
            }
        });
    }

    private boolean isTenantSetByAnnotation(RoutingContext context, String tenantId) {
        return annotationBasedTenantResolutionEnabled &&
                tenantId != null && context.get(OidcUtils.TENANT_ID_SET_BY_ANNOTATION) != null;
    }

    private TenantConfigContext getStaticTenantContext(String tenantId) {
        var configContext = tenantId != null ? tenantConfigBean.getStaticTenant(tenantId) : null;
        if (configContext == null) {
            if (tenantId != null && !tenantId.isEmpty() && !OidcUtils.DEFAULT_TENANT_ID.equals(tenantId)) {
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
        if (isTenantSetByAnnotation(context, context.get(OidcUtils.TENANT_ID_ATTRIBUTE))) {
            return Uni.createFrom().nullItem();
        }
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
            public Uni<TenantConfigContext> apply(OidcTenantConfig tenantConfig) {
                if (tenantConfig != null) {
                    var tenantId = tenantConfig.tenantId()
                            .orElseThrow(() -> new OIDCException("Tenant configuration must have tenant id"));
                    var tenantContext = tenantConfigBean.getDynamicTenant(tenantId);
                    if (tenantContext == null) {
                        return tenantConfigBean.createDynamicTenantContext(tenantConfig);
                    } else if (tenantContext.getOidcTenantConfig() != tenantConfig) {

                        Uni<TenantConfigContext> dynamicContextUni = null;
                        if (Boolean.valueOf(context.get(REPLACE_TENANT_CONFIG_CONTEXT))) {
                            // replace the context and reconnect
                            dynamicContextUni = tenantConfigBean.replaceDynamicTenantContext(tenantConfig);
                        } else {
                            // update the context without reconnect
                            dynamicContextUni = tenantConfigBean.updateDynamicTenantContext(tenantConfig);
                        }
                        final Uni<TenantConfigContext> contextUni = dynamicContextUni;
                        if (Boolean.valueOf(context.get(REMOVE_SESSION_COOKIE))) {
                            final String message = """
                                    Requesting re-authentication for the tenant %s to align with the new dynamic tenant context requirements.
                                    """
                                    .formatted(tenantId);
                            LOG.debug(message);
                            // Clear the session cookie using the current configuration
                            return Uni.createFrom().item(tenantContext.getOidcTenantConfig())
                                    .chain(new Function<OidcTenantConfig, Uni<? extends Void>>() {
                                        @Override
                                        public Uni<Void> apply(OidcTenantConfig oidcConfig) {
                                            OidcUtils.setClearSiteData(context, oidcConfig);
                                            return OidcUtils.removeSessionCookie(context, oidcConfig, tokenStateManager.get());
                                        }
                                    })
                                    // Deal with updating or replacing the dynamic context
                                    .chain(() -> contextUni)
                                    // Finally, request re-authentication
                                    .onItem().failWith(() -> new AuthenticationFailedException(message));
                        } else {
                            return dynamicContextUni;
                        }
                    } else {
                        return Uni.createFrom().item(tenantContext);
                    }
                } else {
                    final String tenantId = context.get(OidcUtils.TENANT_ID_ATTRIBUTE);
                    if (tenantId != null && !isTenantSetByAnnotation(context, tenantId)) {
                        TenantConfigContext tenantContext = tenantConfigBean.getDynamicTenant(tenantId);
                        if (tenantContext != null) {
                            return Uni.createFrom().item(tenantContext);
                        }
                    }
                }
                return Uni.createFrom().nullItem();
            }
        });
    }

    boolean isEnableHttpForwardedPrefix() {
        return enableHttpForwardedPrefix;
    }

    Map<String, BackChannelLogoutTokenCache> getBackChannelLogoutTokens() {
        return backChannelLogoutTokens;
    }

    public TenantConfigBean getTenantConfigBean() {
        return tenantConfigBean;
    }

    public JavaScriptRequestChecker getJavaScriptRequestChecker() {
        return javaScriptRequestChecker.isResolvable() ? javaScriptRequestChecker.get() : null;
    }

    public OidcTenantConfig getResolvedConfig(String sessionTenantId) {
        if (OidcUtils.DEFAULT_TENANT_ID.equals(sessionTenantId)) {
            return tenantConfigBean.getDefaultTenant().getOidcTenantConfig();
        }

        var tenant = tenantConfigBean.getStaticTenant(sessionTenantId);
        if (tenant == null) {
            tenant = tenantConfigBean.getDynamicTenant(sessionTenantId);
        }
        return tenant != null ? tenant.getOidcTenantConfig() : null;
    }

    public String getRootPath() {
        return rootPath;
    }

}
