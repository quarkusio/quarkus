package io.quarkus.oidc.runtime;

import static io.quarkus.oidc.runtime.OidcProvider.ANY_ISSUER;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;

import jakarta.enterprise.inject.Instance;

import org.eclipse.microprofile.jwt.Claims;
import org.jboss.logging.Logger;

import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.TenantResolver;
import io.quarkus.vertx.http.runtime.security.ImmutablePathMatcher;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

final class StaticTenantResolver {

    private static final Logger LOG = Logger.getLogger(StaticTenantResolver.class);

    private final TenantResolver[] staticTenantResolvers;
    private final IssuerBasedTenantResolver issuerBasedTenantResolver;

    StaticTenantResolver(TenantConfigBean tenantConfigBean, String rootPath, boolean resolveTenantsWithIssuer,
            Instance<TenantResolver> tenantResolverInstance) {
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
            staticTenantResolvers.add(new DefaultStaticTenantResolver(tenantConfigBean));
        }

        this.staticTenantResolvers = staticTenantResolvers.toArray(new TenantResolver[0]);

        // 4. issuer-based tenant resolver
        if (resolveTenantsWithIssuer) {
            this.issuerBasedTenantResolver = IssuerBasedTenantResolver.of(
                    tenantConfigBean.getStaticTenantsConfig(), tenantConfigBean.getDefaultTenant());
        } else {
            this.issuerBasedTenantResolver = null;
        }
    }

    Uni<String> resolve(RoutingContext context) {
        for (TenantResolver resolver : staticTenantResolvers) {
            final String tenantId = resolver.resolve(context);
            if (tenantId != null) {
                return Uni.createFrom().item(tenantId);
            }
        }

        if (issuerBasedTenantResolver != null) {
            return issuerBasedTenantResolver.resolveTenant(context);
        }

        return Uni.createFrom().nullItem();
    }

    private static final class DefaultStaticTenantResolver implements TenantResolver {

        private final TenantConfigBean tenantConfigBean;

        private DefaultStaticTenantResolver(TenantConfigBean tenantConfigBean) {
            this.tenantConfigBean = tenantConfigBean;
        }

        @Override
        public String resolve(RoutingContext context) {
            String[] pathSegments = context.request().path().split("/");
            if (pathSegments.length > 0) {
                String lastPathSegment = pathSegments[pathSegments.length - 1];
                if (tenantConfigBean.getStaticTenant(lastPathSegment) != null) {
                    LOG.debugf(
                            "Tenant id '%s' is selected on the '%s' request path", lastPathSegment, context.normalizedPath());
                    return lastPathSegment;
                }
            }
            return null;
        }
    }

    private static final class PathMatchingTenantResolver implements TenantResolver {
        private static final String DEFAULT_TENANT = "PathMatchingTenantResolver#DefaultTenant";
        private final ImmutablePathMatcher<String> staticTenantPaths;

        private PathMatchingTenantResolver(ImmutablePathMatcher<String> staticTenantPaths) {
            this.staticTenantPaths = staticTenantPaths;
        }

        private static PathMatchingTenantResolver of(Map<String, TenantConfigContext> staticTenantsConfig, String rootPath,
                TenantConfigContext defaultTenant) {
            final var builder = ImmutablePathMatcher.<String> builder().rootPath(rootPath);
            addPath(DEFAULT_TENANT, defaultTenant.oidcConfig(), builder);
            for (Map.Entry<String, TenantConfigContext> e : staticTenantsConfig.entrySet()) {
                addPath(e.getKey(), e.getValue().oidcConfig(), builder);
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

    private static final class IssuerBasedTenantResolver {

        private final TenantConfigContext[] tenantConfigContexts;
        private final boolean detectedTenantWithoutMetadata;
        private final Map<String, AtomicBoolean> tenantToRetry;

        private IssuerBasedTenantResolver(TenantConfigContext[] tenantConfigContexts, boolean detectedTenantWithoutMetadata,
                Map<String, AtomicBoolean> tenantToRetry) {
            this.tenantConfigContexts = tenantConfigContexts;
            this.detectedTenantWithoutMetadata = detectedTenantWithoutMetadata;
            this.tenantToRetry = tenantToRetry;
        }

        private Uni<String> resolveTenant(RoutingContext context) {
            return resolveTenant(context, 0);
        }

        private Uni<String> resolveTenant(RoutingContext context, int index) {
            if (index == tenantConfigContexts.length) {
                return Uni.createFrom().nullItem();
            }
            var tenantContext = tenantConfigContexts[index];
            if (detectedTenantWithoutMetadata) {
                // this is static tenant that didn't have OIDC metadata available at startup

                if (tenantContext.getOidcMetadata() == null) {
                    if (tenantContext.ready()) {
                        return resolveTenant(context, index + 1);
                    }

                    if (!tryToInitialize(tenantContext)) {
                        return resolveTenant(context, index + 1);
                    }

                    return tenantContext.initialize()
                            .onItemOrFailure()
                            .transformToUni(new BiFunction<TenantConfigContext, Throwable, Uni<? extends String>>() {
                                @Override
                                public Uni<String> apply(TenantConfigContext newContext, Throwable throwable) {
                                    if (throwable != null) {
                                        return resolveTenant(context, index + 1);
                                    }
                                    if (newContext.ready() && !isTenantWithoutIssuer(newContext)) {
                                        return getTenantId(newContext, context, index);
                                    }
                                    return resolveTenant(context, index + 1);
                                }
                            });
                }

                if (isTenantWithoutIssuer(tenantContext)) {
                    return resolveTenant(context, index + 1);
                }
            }

            return getTenantId(tenantContext, context, index);
        }

        private Uni<String> getTenantId(TenantConfigContext tenantContext, RoutingContext context, int index) {
            var tenantId = getTenantId(context, tenantContext);
            if (tenantId == null) {
                return resolveTenant(context, index + 1);
            }
            return Uni.createFrom().item(tenantId);
        }

        /**
         * When static tenant couldn't be initialized on Quarkus application startup,
         * this strategy permits one more attempt on the first request when the issuer-based tenant resolver is applied.
         */
        private boolean tryToInitialize(TenantConfigContext context) {
            var tenantId = context.oidcConfig().tenantId.get();
            return this.tenantToRetry.get(tenantId).compareAndExchange(true, false);
        }

        private static String getTenantId(RoutingContext context, TenantConfigContext tenantContext) {
            final String token = OidcUtils.extractBearerToken(context, tenantContext.oidcConfig());
            if (token != null && !OidcUtils.isOpaqueToken(token)) {
                final var tokenJson = OidcUtils.decodeJwtContent(token);
                if (tokenJson != null) {

                    final String iss = tokenJson.getString(Claims.iss.name());
                    if (tenantContext.getOidcMetadata().getIssuer().equals(iss)) {
                        OidcUtils.storeExtractedBearerToken(context, token);

                        final String tenantId = tenantContext.oidcConfig().tenantId.get();
                        LOG.debugf("Resolved the '%s' OIDC tenant based on the matching issuer '%s'", tenantId, iss);
                        return tenantId;
                    }
                }
            }
            return null;
        }

        private static boolean isTenantWithoutIssuer(TenantConfigContext tenantContext) {
            return tenantContext.getOidcMetadata().getIssuer() == null
                    || ANY_ISSUER.equals(tenantContext.getOidcMetadata().getIssuer());
        }

        private static IssuerBasedTenantResolver of(Map<String, TenantConfigContext> tenantConfigContexts) {
            var contextsWithIssuer = new ArrayList<TenantConfigContext>();
            boolean detectedTenantWithoutMetadata = false;
            Map<String, AtomicBoolean> tenantToRetry = new HashMap<>();
            for (TenantConfigContext context : tenantConfigContexts.values()) {
                if (context.oidcConfig().tenantEnabled && !OidcUtils.isWebApp(context.oidcConfig())) {
                    if (context.getOidcMetadata() == null) {
                        // if the tenant metadata are not available, we can't decide now
                        detectedTenantWithoutMetadata = true;
                        contextsWithIssuer.add(context);
                        tenantToRetry.put(context.oidcConfig().tenantId.get(), new AtomicBoolean(true));
                    } else if (context.getOidcMetadata().getIssuer() != null
                            && !ANY_ISSUER.equals(context.getOidcMetadata().getIssuer())) {
                        contextsWithIssuer.add(context);
                    }
                }
            }
            if (contextsWithIssuer.isEmpty()) {
                return null;
            } else {
                var tenantInitStrategy = detectedTenantWithoutMetadata ? Map.copyOf(tenantToRetry) : null;
                return new IssuerBasedTenantResolver(contextsWithIssuer.toArray(new TenantConfigContext[0]),
                        detectedTenantWithoutMetadata, tenantInitStrategy);
            }
        }

        private static IssuerBasedTenantResolver of(Map<String, TenantConfigContext> staticTenantsConfig,
                TenantConfigContext defaultTenant) {
            Map<String, TenantConfigContext> tenantConfigContexts = new HashMap<>(staticTenantsConfig);
            tenantConfigContexts.put(OidcUtils.DEFAULT_TENANT_ID, defaultTenant);
            var issuerTenantResolver = IssuerBasedTenantResolver.of(tenantConfigContexts);
            if (issuerTenantResolver != null) {
                return issuerTenantResolver;
            } else {
                LOG.debug("The 'quarkus.oidc.resolve-tenants-with-issuer' configuration property is set to true, "
                        + "but no static tenant supports this feature. To use this feature, please configure at least "
                        + "one static tenant with the discovered or configured issuer and set either 'service' or "
                        + "'hybrid' application type");
                return null;
            }
        }
    }

}
