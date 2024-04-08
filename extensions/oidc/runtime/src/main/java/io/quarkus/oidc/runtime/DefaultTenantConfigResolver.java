package io.quarkus.oidc.runtime;

import static io.quarkus.oidc.runtime.OidcProvider.ANY_ISSUER;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
import org.eclipse.microprofile.jwt.Claims;
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
import io.quarkus.vertx.http.runtime.security.ImmutablePathMatcher;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class DefaultTenantConfigResolver {

    private static final Logger LOG = Logger.getLogger(DefaultTenantConfigResolver.class);
    private static final String CURRENT_STATIC_TENANT_ID = "static.tenant.id";
    private static final String CURRENT_STATIC_TENANT_ID_NULL = "static.tenant.id.null";
    private static final String CURRENT_DYNAMIC_TENANT_CONFIG = "dynamic.tenant.config";
    private final ConcurrentHashMap<String, BackChannelLogoutTokenCache> backChannelLogoutTokens = new ConcurrentHashMap<>();
    private final BlockingTaskRunner<OidcTenantConfig> blockingRequestContext;
    private final boolean securityEventObserved;
    private final TenantConfigBean tenantConfigBean;
    private final TenantResolver[] staticTenantResolvers;
    private final boolean annotationBasedTenantResolutionEnabled;

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
        this.staticTenantResolvers = prepareStaticTenantResolvers(tenantConfigBean, rootPath, tenantResolverInstance,
                resolveTenantsWithIssuer, new DefaultStaticTenantResolver());
        this.annotationBasedTenantResolutionEnabled = Boolean.getBoolean(OidcUtils.ANNOTATION_BASED_TENANT_RESOLUTION_ENABLED);
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
                .map(new Function<OidcTenantConfig, OidcTenantConfig>() {
                    @Override
                    public OidcTenantConfig apply(OidcTenantConfig tenantConfig) {
                        if (tenantConfig == null) {

                            final String tenantId = context.get(OidcUtils.TENANT_ID_ATTRIBUTE);

                            if (tenantId != null && !isTenantSetByAnnotation(context, tenantId)) {
                                TenantConfigContext tenantContext = tenantConfigBean.getDynamicTenantsConfig().get(tenantId);
                                if (tenantContext != null) {
                                    // Dynamic map may contain the static contexts initialized on demand,
                                    if (tenantConfigBean.getStaticTenantsConfig().containsKey(tenantId)) {
                                        context.put(CURRENT_STATIC_TENANT_ID, tenantId);
                                    }
                                    return tenantContext.getOidcTenantConfig();
                                }
                            }

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
        return initializeStaticTenantIfContextNotReady(getStaticTenantContext(tenantId));
    }

    Uni<TenantConfigContext> resolveContext(RoutingContext context) {
        return getDynamicTenantContext(context).onItem().ifNull().switchTo(new Supplier<Uni<? extends TenantConfigContext>>() {
            @Override
            public Uni<? extends TenantConfigContext> get() {
                return initializeStaticTenantIfContextNotReady(getStaticTenantContext(context));
            }
        });
    }

    private Uni<TenantConfigContext> initializeStaticTenantIfContextNotReady(TenantConfigContext tenantContext) {
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
            tenantId = resolveStaticTenantId(context);
            if (tenantId != null) {
                context.put(CURRENT_STATIC_TENANT_ID, tenantId);
            } else {
                context.put(CURRENT_STATIC_TENANT_ID_NULL, true);
            }
        }

        return getStaticTenantContext(tenantId);
    }

    private String resolveStaticTenantId(RoutingContext context) {
        String tenantId = context.get(OidcUtils.TENANT_ID_ATTRIBUTE);
        if (isTenantSetByAnnotation(context, tenantId)) {
            return tenantId;
        }

        for (var staticTenantResolver : staticTenantResolvers) {
            tenantId = staticTenantResolver.resolve(context);
            if (tenantId != null) {
                return tenantId;
            }
        }

        return context.get(OidcUtils.TENANT_ID_ATTRIBUTE);
    }

    private boolean isTenantSetByAnnotation(RoutingContext context, String tenantId) {
        return annotationBasedTenantResolutionEnabled &&
                tenantId != null && context.get(OidcUtils.TENANT_ID_SET_BY_ANNOTATION) != null;
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
                } else {
                    final String tenantId = context.get(OidcUtils.TENANT_ID_ATTRIBUTE);
                    if (tenantId != null && !isTenantSetByAnnotation(context, tenantId)) {
                        TenantConfigContext tenantContext = tenantConfigBean.getDynamicTenantsConfig().get(tenantId);
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

    public Map<String, BackChannelLogoutTokenCache> getBackChannelLogoutTokens() {
        return backChannelLogoutTokens;
    }

    public TenantConfigBean getTenantConfigBean() {
        return tenantConfigBean;
    }

    public JavaScriptRequestChecker getJavaScriptRequestChecker() {
        return javaScriptRequestChecker.isResolvable() ? javaScriptRequestChecker.get() : null;
    }

    private static TenantResolver[] prepareStaticTenantResolvers(TenantConfigBean tenantConfigBean, String rootPath,
            Instance<TenantResolver> tenantResolverInstance, boolean resolveTenantsWithIssuer,
            TenantResolver defaultStaticTenantResolver) {
        List<TenantResolver> staticTenantResolvers = new ArrayList<>();
        // STATIC TENANT RESOLVERS BY PRIORITY:
        // 0. annotation based resolver

        // 1. custom tenant resolver
        if (tenantResolverInstance.isResolvable()) {
            if (tenantResolverInstance.isAmbiguous()) {
                throw new IllegalStateException("Multiple " + TenantResolver.class + " beans registered");
            }
            staticTenantResolvers.add(tenantResolverInstance.get());
        }

        // 2. path-matching tenant resolver
        var pathMatchingTenantResolver = PathMatchingTenantResolver.of(tenantConfigBean.getStaticTenantsConfig(), rootPath,
                tenantConfigBean.getDefaultTenant());
        if (pathMatchingTenantResolver != null) {
            staticTenantResolvers.add(pathMatchingTenantResolver);
        }

        // 3. default static tenant resolver
        if (!tenantConfigBean.getStaticTenantsConfig().isEmpty()) {
            staticTenantResolvers.add(defaultStaticTenantResolver);
        }

        // 4. issuer-based tenant resolver
        if (resolveTenantsWithIssuer) {
            IssuerBasedTenantResolver.addIssuerBasedTenantResolver(staticTenantResolvers,
                    tenantConfigBean.getStaticTenantsConfig(), tenantConfigBean.getDefaultTenant());
        }

        return staticTenantResolvers.toArray(new TenantResolver[0]);
    }

    private class DefaultStaticTenantResolver implements TenantResolver {

        @Override
        public String resolve(RoutingContext context) {
            String[] pathSegments = context.request().path().split("/");
            if (pathSegments.length > 0) {
                String lastPathSegment = pathSegments[pathSegments.length - 1];
                if (tenantConfigBean.getStaticTenantsConfig().containsKey(lastPathSegment)) {
                    LOG.debugf(
                            "Tenant id '%s' is selected on the '%s' request path", lastPathSegment, context.normalizedPath());
                    return lastPathSegment;
                }
            }
            return null;
        }
    }

    private static class PathMatchingTenantResolver implements TenantResolver {
        private static final String DEFAULT_TENANT = "PathMatchingTenantResolver#DefaultTenant";
        private final ImmutablePathMatcher<String> staticTenantPaths;

        private PathMatchingTenantResolver(ImmutablePathMatcher<String> staticTenantPaths) {
            this.staticTenantPaths = staticTenantPaths;
        }

        private static PathMatchingTenantResolver of(Map<String, TenantConfigContext> staticTenantsConfig, String rootPath,
                TenantConfigContext defaultTenant) {
            final var builder = ImmutablePathMatcher.<String> builder().rootPath(rootPath);
            addPath(DEFAULT_TENANT, defaultTenant.oidcConfig, builder);
            for (Map.Entry<String, TenantConfigContext> e : staticTenantsConfig.entrySet()) {
                addPath(e.getKey(), e.getValue().oidcConfig, builder);
            }
            return builder.hasPaths() ? new PathMatchingTenantResolver(builder.build()) : null;
        }

        @Override
        public String resolve(RoutingContext context) {
            String tenantId = staticTenantPaths.match(context.normalizedPath()).getValue();
            if (tenantId != null) {
                LOG.debugf(
                        "Tenant id '%s' is selected on the '%s' request path", tenantId, context.normalizedPath());
                return tenantId;
            }
            return null;
        }

        private static ImmutablePathMatcher.ImmutablePathMatcherBuilder<String> addPath(String tenant, OidcTenantConfig config,
                ImmutablePathMatcher.ImmutablePathMatcherBuilder<String> builder) {
            if (config != null && config.tenantPaths.isPresent()) {
                for (String path : config.tenantPaths.get()) {
                    builder.addPath(path, tenant);
                }
            }
            return builder;
        }
    }

    public OidcTenantConfig getResolvedConfig(String sessionTenantId) {
        if (OidcUtils.DEFAULT_TENANT_ID.equals(sessionTenantId)) {
            return tenantConfigBean.getDefaultTenant().getOidcTenantConfig();
        }

        if (tenantConfigBean.getStaticTenantsConfig().containsKey(sessionTenantId)) {
            return tenantConfigBean.getStaticTenantsConfig().get(sessionTenantId).getOidcTenantConfig();
        }

        if (tenantConfigBean.getDynamicTenantsConfig().containsKey(sessionTenantId)) {
            return tenantConfigBean.getDynamicTenantsConfig().get(sessionTenantId).getOidcTenantConfig();
        }
        return null;
    }

    private static final class IssuerBasedTenantResolver implements TenantResolver {

        private final TenantConfigContext[] tenantConfigContexts;
        private final boolean detectedTenantWithoutMetadata;

        private IssuerBasedTenantResolver(TenantConfigContext[] tenantConfigContexts, boolean detectedTenantWithoutMetadata) {
            this.tenantConfigContexts = tenantConfigContexts;
            this.detectedTenantWithoutMetadata = detectedTenantWithoutMetadata;
        }

        @Override
        public String resolve(RoutingContext context) {
            for (var tenantContext : tenantConfigContexts) {
                if (detectedTenantWithoutMetadata
                        && (tenantContext.getOidcMetadata() == null || tenantContext.getOidcMetadata().getIssuer() == null
                                || ANY_ISSUER.equals(tenantContext.getOidcMetadata().getIssuer()))) {
                    // this is static tenant that didn't have OIDC metadata available at startup
                    continue;
                }

                final String token = OidcUtils.extractBearerToken(context, tenantContext.oidcConfig);
                if (token != null && !OidcUtils.isOpaqueToken(token)) {
                    final var tokenJson = OidcUtils.decodeJwtContent(token);
                    if (tokenJson != null) {

                        final String iss = tokenJson.getString(Claims.iss.name());
                        if (tenantContext.getOidcMetadata().getIssuer().equals(iss)) {
                            OidcUtils.storeExtractedBearerToken(context, token);

                            final String tenantId = tenantContext.oidcConfig.tenantId.get();
                            LOG.debugf("Resolved the '%s' OIDC tenant based on the matching issuer '%s'", tenantId, iss);
                            return tenantId;
                        }
                    }
                }
            }
            return null;
        }

        private static TenantResolver of(Map<String, TenantConfigContext> tenantConfigContexts) {
            var contextsWithIssuer = new ArrayList<TenantConfigContext>();
            boolean detectedTenantWithoutMetadata = false;
            for (TenantConfigContext context : tenantConfigContexts.values()) {
                if (context.oidcConfig.tenantEnabled && !OidcUtils.isWebApp(context.oidcConfig)) {
                    if (context.getOidcMetadata() == null) {
                        // if the tenant metadata are not available, we can't decide now
                        detectedTenantWithoutMetadata = true;
                        contextsWithIssuer.add(context);
                    } else if (context.getOidcMetadata().getIssuer() != null
                            && !ANY_ISSUER.equals(context.getOidcMetadata().getIssuer())) {
                        contextsWithIssuer.add(context);
                    }
                }
            }
            if (contextsWithIssuer.isEmpty()) {
                return null;
            } else {
                return new IssuerBasedTenantResolver(contextsWithIssuer.toArray(new TenantConfigContext[0]),
                        detectedTenantWithoutMetadata);
            }
        }

        private static void addIssuerBasedTenantResolver(List<TenantResolver> resolvers,
                Map<String, TenantConfigContext> staticTenantsConfig, TenantConfigContext defaultTenant) {
            Map<String, TenantConfigContext> tenantConfigContexts = new HashMap<>(staticTenantsConfig);
            tenantConfigContexts.put(OidcUtils.DEFAULT_TENANT_ID, defaultTenant);
            var issuerTenantResolver = IssuerBasedTenantResolver.of(tenantConfigContexts);
            if (issuerTenantResolver != null) {
                resolvers.add(issuerTenantResolver);
            } else {
                LOG.debug("The 'quarkus.oidc.resolve-tenants-with-issuer' configuration property is set to true, "
                        + "but no static tenant supports this feature. To use this feature, please configure at least "
                        + "one static tenant with the discovered or configured issuer and set either 'service' or "
                        + "'hybrid' application type");
            }
        }
    }

}
