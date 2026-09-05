package io.quarkus.oidc.runtime;

import static io.quarkus.oidc.runtime.OidcProvider.ANY_ISSUER;
import static io.vertx.core.http.HttpHeaders.AUTHORIZATION;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;

import jakarta.enterprise.inject.Instance;

import org.eclipse.microprofile.jwt.Claims;
import org.jboss.logging.Logger;

import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.TenantResolver;
import io.quarkus.oidc.common.runtime.OidcCommonUtils;
import io.quarkus.vertx.http.runtime.security.HttpSecurityUtils;
import io.quarkus.vertx.http.runtime.security.ImmutablePathMatcher;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

final class StaticTenantResolver {

    private static final Logger LOG = Logger.getLogger(StaticTenantResolver.class);

    private final TenantResolver[] staticTenantResolversGroup1;
    private final IssuerBasedTenantResolver issuerBasedTenantResolver;
    private final TenantResolver[] staticTenantResolversGroup2;

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

        this.staticTenantResolversGroup1 = staticTenantResolvers.toArray(new TenantResolver[0]);

        // 3. issuer-based tenant resolver
        if (resolveTenantsWithIssuer) {
            this.issuerBasedTenantResolver = IssuerBasedTenantResolver.of(
                    tenantConfigBean.getStaticTenantsConfig(), tenantConfigBean.getDefaultTenant());
        } else {
            this.issuerBasedTenantResolver = null;
        }

        staticTenantResolvers.clear();

        // 4. header-based tenant resolver
        var headerBasedTenantResolver = HeaderBasedTenantResolver.of(tenantConfigBean);
        if (headerBasedTenantResolver != null) {
            staticTenantResolvers.add(headerBasedTenantResolver);
        }

        // 5. default static tenant resolver
        if (!tenantConfigBean.getStaticTenantsConfig().isEmpty()) {
            staticTenantResolvers.add(new DefaultStaticTenantResolver(tenantConfigBean));
        }

        this.staticTenantResolversGroup2 = staticTenantResolvers.toArray(new TenantResolver[0]);
    }

    Uni<String> resolve(RoutingContext context) {
        for (TenantResolver resolver : staticTenantResolversGroup1) {
            final String tenantId = resolver.resolve(context);
            if (tenantId != null) {
                return Uni.createFrom().item(tenantId);
            }
        }

        if (issuerBasedTenantResolver != null) {
            return issuerBasedTenantResolver.resolveTenant(context)
                    .map(resolvedTenant -> {
                        if (resolvedTenant != null) {
                            return resolvedTenant;
                        }
                        for (TenantResolver resolver : staticTenantResolversGroup2) {
                            final String tenantId = resolver.resolve(context);
                            if (tenantId != null) {
                                return tenantId;
                            }
                        }
                        return null;
                    });
        }

        for (TenantResolver resolver : staticTenantResolversGroup2) {
            final String tenantId = resolver.resolve(context);
            if (tenantId != null) {
                return Uni.createFrom().item(tenantId);
            }
        }

        return Uni.createFrom().nullItem();
    }

    /**
     * Decides whether a not-yet-initialized issuer-resolved tenant may be (re-)initialized now.
     *
     * @param previousAttemptMillis epoch millis of the last attempt, or 0 if none yet
     * @param nowMillis current epoch millis
     * @param retryIntervalMillis minimum interval between attempts; <= 0 disables retries after the first one
     */
    static boolean shouldRetryInitialization(long previousAttemptMillis, long nowMillis, long retryIntervalMillis) {
        if (previousAttemptMillis == 0) {
            return true; // first attempt, on the first request
        }
        return retryIntervalMillis > 0 && (nowMillis - previousAttemptMillis) >= retryIntervalMillis;
    }

    private static final class DefaultStaticTenantResolver implements TenantResolver {
        private static final String PATH_SEPARATOR = "/";
        private final TenantConfigBean tenantConfigBean;

        private DefaultStaticTenantResolver(TenantConfigBean tenantConfigBean) {
            this.tenantConfigBean = tenantConfigBean;
        }

        @Override
        public String resolve(RoutingContext context) {
            String[] pathSegments = HttpSecurityUtils.normalizePath(context.normalizedPath())
                    .split(PATH_SEPARATOR);
            for (String canonicalSegment : pathSegments) {
                if (tenantConfigBean.getStaticTenant(canonicalSegment) != null) {
                    LOG.debugf(
                            "Tenant id '%s' is selected on the '%s' request path", canonicalSegment, context.normalizedPath());
                    return canonicalSegment;
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
            String canonicalPath = HttpSecurityUtils.normalizePath(context.normalizedPath());
            String tenantId = staticTenantPaths.match(canonicalPath).getValue();
            if (tenantId != null) {
                LOG.debugf(
                        "Tenant id '%s' is selected on the '%s' request path", tenantId, canonicalPath);
                return tenantId;
            }
            return null;
        }

        private static ImmutablePathMatcher.ImmutablePathMatcherBuilder<String> addPath(String tenant, OidcTenantConfig config,
                ImmutablePathMatcher.ImmutablePathMatcherBuilder<String> builder) {
            if (config != null && config.tenantPaths().isPresent()) {
                for (String path : config.tenantPaths().get()) {
                    builder.addPath(path, tenant);
                }
            }
            return builder;
        }
    }

    private static final class IssuerBasedTenantResolver {

        private final TenantConfigContext[] tenantConfigContexts;
        private final boolean detectedTenantWithoutMetadata;
        private final Map<String, AtomicLong> lastInitAttempt;

        private IssuerBasedTenantResolver(TenantConfigContext[] tenantConfigContexts, boolean detectedTenantWithoutMetadata,
                Map<String, AtomicLong> lastInitAttempt) {
            this.tenantConfigContexts = tenantConfigContexts;
            this.detectedTenantWithoutMetadata = detectedTenantWithoutMetadata;
            this.lastInitAttempt = lastInitAttempt;
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
                        if (isTenantWithIssuer(tenantContext)) {
                            return getTenantId(tenantContext, context, index);
                        }
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
                                    if (newContext.ready() && isTenantWithIssuer(newContext)) {
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
         * A static tenant whose OIDC metadata was not available at application startup is (re-)initialized
         * lazily while resolving a tenant from the bearer token issuer.
         * <p>
         * The first attempt happens on the first request. Further attempts are only made when
         * this tenant's 'initialization-retry-interval' is positive, and then at most once per that
         * interval per tenant, allowing the tenant to recover after the OIDC provider becomes available
         * again without an application restart.
         */
        private boolean tryToInitialize(TenantConfigContext context) {
            var tenantId = context.oidcConfig().tenantId().get();
            AtomicLong lastAttempt = lastInitAttempt.get(tenantId);
            long previous = lastAttempt.get();
            long now = System.currentTimeMillis();
            long retryIntervalMillis = context.oidcConfig().initializationRetryInterval().toMillis();
            if (shouldRetryInitialization(previous, now, retryIntervalMillis)) {
                // only one concurrent request wins the right to (re-)initialize this tenant
                return lastAttempt.compareAndSet(previous, now);
            }
            return false;
        }

        private static String getTenantId(RoutingContext context, TenantConfigContext tenantContext) {
            final String token = OidcUtils.extractBearerToken(context, tenantContext.oidcConfig());
            if (token != null && !OidcUtils.isOpaqueToken(token)) {
                final var tokenJson = OidcCommonUtils.decodeJwtContent(token);
                if (tokenJson != null) {

                    final String iss = tokenJson.getString(Claims.iss.name());
                    if (iss != null && iss.equals(getIssuer(tenantContext))) {

                        final String tenantId = tenantContext.oidcConfig().tenantId().get();

                        if (!requiredClaimsMatch(tenantContext.oidcConfig().token().requiredClaims(), tokenJson)) {
                            LOG.debugf(
                                    "OIDC tenant '%s' issuer matches the token issuer '%s' but does not match the token required claims",
                                    tenantId, iss);
                            return null;
                        }

                        OidcUtils.storeExtractedBearerToken(context, token);
                        LOG.debugf("Resolved the '%s' OIDC tenant based on the matching issuer '%s'", tenantId, iss);
                        return tenantId;
                    }
                }
            }
            return null;
        }

        private static boolean requiredClaimsMatch(Map<String, Set<String>> requiredClaims, JsonObject tokenJson) {
            for (Map.Entry<String, Set<String>> entry : requiredClaims.entrySet()) {
                Set<String> requiredClaimSet = entry.getValue();
                String claimName = entry.getKey();
                if (requiredClaimSet.size() == 1) {
                    String actualClaimValueAsStr;
                    try {
                        actualClaimValueAsStr = tokenJson.getString(claimName);
                    } catch (Exception ex) {
                        actualClaimValueAsStr = null;
                    }
                    if (actualClaimValueAsStr != null && requiredClaimSet.contains(actualClaimValueAsStr)) {
                        continue;
                    }
                }
                final JsonArray actualClaimValues;
                try {
                    actualClaimValues = tokenJson.getJsonArray(claimName);
                } catch (Exception e) {
                    return false;
                }
                if (actualClaimValues == null) {
                    return false;
                }
                outer: for (String requiredClaimValue : requiredClaimSet) {
                    for (int i = 0; i < actualClaimValues.size(); i++) {
                        try {
                            String actualClaimValue = actualClaimValues.getString(i);
                            if (requiredClaimValue.equals(actualClaimValue)) {
                                continue outer;
                            }
                        } catch (Exception ignored) {
                            // try next actual claim value
                        }
                    }
                    return false;
                }
            }
            return true;
        }

        private static boolean isTenantWithoutIssuer(TenantConfigContext tenantContext) {
            return getIssuer(tenantContext) == null;
        }

        private static boolean isTenantWithIssuer(TenantConfigContext tenantContext) {
            return getIssuer(tenantContext) != null;
        }

        private static IssuerBasedTenantResolver of(Map<String, TenantConfigContext> tenantConfigContexts) {
            var contextsWithIssuer = new ArrayList<TenantConfigContext>();
            boolean detectedTenantWithoutMetadata = false;
            Map<String, AtomicLong> lastInitAttempt = new HashMap<>();
            for (TenantConfigContext context : tenantConfigContexts.values()) {
                if (context.oidcConfig().tenantEnabled() && !OidcUtils.isWebApp(context.oidcConfig())) {
                    if (context.getOidcMetadata() == null) {
                        // if the tenant metadata are not available, we can't decide now
                        detectedTenantWithoutMetadata = true;
                        contextsWithIssuer.add(context);
                        lastInitAttempt.put(context.oidcConfig().tenantId().get(), new AtomicLong(0));
                    } else if (isTenantWithIssuer(context)) {
                        contextsWithIssuer.add(context);
                    }
                }
            }
            if (contextsWithIssuer.isEmpty()) {
                return null;
            } else {
                var attempts = detectedTenantWithoutMetadata ? Map.copyOf(lastInitAttempt) : Map.<String, AtomicLong> of();
                return new IssuerBasedTenantResolver(contextsWithIssuer.toArray(new TenantConfigContext[0]),
                        detectedTenantWithoutMetadata, attempts);
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

        private static String getIssuer(TenantConfigContext tenantContext) {
            if (tenantContext.getOidcMetadata() != null) {
                String issuer = tenantContext.getOidcMetadata().getIssuer();
                if (issuer != null && isNotAnyIssuer(issuer)) {
                    return issuer;
                }
            } else if (tenantContext.oidcConfig() != null && tenantContext.oidcConfig().token().issuer()
                    .filter(IssuerBasedTenantResolver::isNotAnyIssuer).isPresent()) {
                return tenantContext.oidcConfig().token().issuer().get();
            }
            return null;
        }

        private static boolean isNotAnyIssuer(String issuer) {
            return !ANY_ISSUER.equals(issuer);
        }
    }

    private static final class HeaderBasedTenantResolver implements TenantResolver {

        private final Map<String, String> headerNameToTenantId;

        private HeaderBasedTenantResolver(Map<String, String> headerNameToTenantId) {
            this.headerNameToTenantId = Map.copyOf(headerNameToTenantId);
        }

        @Override
        public String resolve(RoutingContext context) {
            for (String headerName : context.request().headers().names()) {
                String tenantId = headerNameToTenantId.get(headerName);
                if (tenantId != null) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debugf("Resolved the '%s' OIDC tenant based on the matching header '%s'", tenantId, headerName);
                    }
                    return tenantId;
                }
            }
            return null;
        }

        private static TenantResolver of(TenantConfigBean tenantConfigBean) {
            var tenantsWitEnabledHeaderResolution = tenantConfigBean.getStaticTenantsConfig().values().stream()
                    .map(TenantConfigContext::getOidcTenantConfig)
                    .filter(Objects::nonNull)
                    .filter(c -> c.token().header().isPresent())
                    .filter(c -> !AUTHORIZATION.toString().equalsIgnoreCase(c.token().header().get()))
                    .toList();
            if (tenantsWitEnabledHeaderResolution.isEmpty()) {
                return null;
            }
            var headerToTenant = new HashMap<String, String>();
            for (OidcTenantConfig tc : tenantsWitEnabledHeaderResolution) {
                String headerName = tc.token().header().get();
                String tenantId = tc.tenantId().get();
                String previousTenantId = headerToTenant.putIfAbsent(headerName, tenantId);
                if (previousTenantId != null) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debugf("OIDC tenants '%s' and '%s' are using the same custom HTTP header '%s'. " +
                                "The '%s' tenant will not be resolved based on the custom HTTP header.", tenantId,
                                previousTenantId, headerName, tenantId);
                    }
                    return null;
                }
            }
            return new HeaderBasedTenantResolver(headerToTenant);
        }
    }
}
