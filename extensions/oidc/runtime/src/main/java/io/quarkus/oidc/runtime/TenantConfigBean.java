package io.quarkus.oidc.runtime;

import static java.util.Collections.unmodifiableMap;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.LongSupplier;

import jakarta.enterprise.context.spi.CreationalContext;

import org.jboss.logging.Logger;

import io.quarkus.arc.BeanDestroyer;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.smallrye.mutiny.Uni;

public class TenantConfigBean {
    /*
     * Note: this class is publicly documented on https://quarkus.io/guides/security-oidc-code-flow-authentication.
     */

    private static final Logger LOG = Logger.getLogger(TenantConfigBean.class);

    private final Map<String, TenantConfigContext> staticTenantsConfig;
    private final Map<String, TenantConfigContext> dynamicTenantsConfig;
    private final TenantConfigContext defaultTenant;
    private final TenantContextFactory tenantContextFactory;
    private final LongSupplier clock;
    private final int dynamicTenantLimit;
    // VisibleForTesting
    final AtomicBoolean tenantEvictionRunning = new AtomicBoolean(false);

    @FunctionalInterface
    public interface TenantContextFactory {
        Uni<TenantConfigContext> create(OidcTenantConfig oidcTenantConfig, boolean dynamicTenant, String tenantId);
    }

    TenantConfigBean(
            Map<String, TenantConfigContext> staticTenantsConfig,
            TenantConfigContext defaultTenant,
            LongSupplier clock,
            int dynamicTenantLimit,
            TenantContextFactory tenantContextFactory) {
        this.staticTenantsConfig = new ConcurrentHashMap<>(staticTenantsConfig);
        this.dynamicTenantsConfig = new ConcurrentHashMap<>();
        this.clock = clock;
        this.dynamicTenantLimit = dynamicTenantLimit;
        this.defaultTenant = defaultTenant;
        this.tenantContextFactory = tenantContextFactory;
    }

    Uni<TenantConfigContext> getOrCreateTenantContext(OidcTenantConfig oidcConfig, boolean dynamicTenant) {
        var tenantId = oidcConfig.getTenantId().orElseThrow();
        var tenants = dynamicTenant ? dynamicTenantsConfig : staticTenantsConfig;
        var tenant = tenants.get(tenantId);
        if (tenant == null || !tenant.isReady()) {
            LOG.tracef("Creating %s tenant config for %s", dynamicTenant ? "dynamic" : "static", tenantId);
            if (dynamicTenant && oidcConfig.logout.backchannel.path.isPresent()) {
                throw new ConfigurationException(
                        "BackChannel Logout is currently not supported for dynamic tenants (tenant ID: " + tenantId + ")");
            }
            Uni<TenantConfigContext> uniContext = tenantContextFactory.create(oidcConfig, dynamicTenant, tenantId);
            return uniContext.onItem().transform(
                    new Function<>() {
                        @Override
                        public TenantConfigContext apply(TenantConfigContext t) {
                            LOG.debugf("Updating %s %s tenant config for %s", dynamicTenant ? "dynamic" : "static",
                                    t.isReady() ? "ready" : "not-ready", tenantId);
                            t.lastUsed = clock.getAsLong();
                            TenantConfigContext previous = tenants.put(tenantId, t);
                            if (previous != null) {
                                // Concurrent calls to createTenantContext may race, better "destroy" the previous
                                // provider, if there's one.
                                destroyContext(previous);
                            } else if (dynamicTenant) {
                                enforceDynamicTenantLimit();
                            }
                            return t;
                        }
                    });
        }
        if (dynamicTenant) {
            tenant.lastUsed = clock.getAsLong();
        }
        LOG.tracef("Immediately returning ready %s tenant config for %s", dynamicTenant ? "dynamic" : "static", tenantId);
        return Uni.createFrom().item(tenant);
    }

    /**
     * Returns a static tenant's config context or {@code null}, if the tenant does not exist.
     */
    public TenantConfigContext getStaticTenantConfigContext(String tenantId) {
        return staticTenantsConfig.get(tenantId);
    }

    /**
     * Returns a static tenant's OIDC configuration or {@code null}, if the tenant does not exist.
     */
    public OidcTenantConfig getStaticTenantOidcConfig(String tenantId) {
        var context = getStaticTenantConfigContext(tenantId);
        return context != null ? context.oidcConfig : null;
    }

    /**
     * Returns an unmodifiable map containing the static tenant config contexts by tenant-ID.
     */
    public Map<String, TenantConfigContext> getStaticTenantsConfig() {
        return unmodifiableMap(staticTenantsConfig);
    }

    /**
     * Returns a dynamic tenant's config context or {@code null}, if the tenant does not exist.
     */
    public TenantConfigContext getDynamicTenantConfigContext(String tenantId) {
        TenantConfigContext context = dynamicTenantsConfig.get(tenantId);
        if (context == null) {
            return null;
        }
        context.lastUsed = clock.getAsLong();
        return context;
    }

    /**
     * Returns a dynamic tenant's OIDC configuration or {@code null}, if the tenant does not exist.
     */
    public OidcTenantConfig getDynamicTenantOidcConfig(String tenantId) {
        var context = getDynamicTenantConfigContext(tenantId);
        return context != null ? context.oidcConfig : null;
    }

    /**
     * Returns an unmodifiable map containing the dynamic tenant config contexts by tenant-ID.
     */
    public Map<String, TenantConfigContext> getDynamicTenantsConfig() {
        return unmodifiableMap(dynamicTenantsConfig);
    }

    /**
     * Returns the default tenant's config context.
     */
    public TenantConfigContext getDefaultTenant() {
        return defaultTenant;
    }

    static void destroyContext(TenantConfigContext context) {
        if (context != null && context.provider != null) {
            context.provider.close();
        }
    }

    public static class Destroyer implements BeanDestroyer<TenantConfigBean> {

        @Override
        public void destroy(TenantConfigBean instance, CreationalContext<TenantConfigBean> creationalContext,
                Map<String, Object> params) {
            destroyContext(instance.defaultTenant);
            for (var i : instance.staticTenantsConfig.values()) {
                destroyContext(i);
            }
            for (var i : instance.dynamicTenantsConfig.values()) {
                destroyContext(i);
            }
        }
    }

    record EvictionCandidate(String tenantId, TenantConfigContext context, long lastUsed) {
    }

    /**
     * Enforces the dynamic tenants limit, if configured.
     *
     * <p>
     * Eviction runs at max on one thread at any time.
     *
     * <p>
     * Iterate over all tenants at best only once, unless an eviction candidate was used during the eviction run.
     */
    private void enforceDynamicTenantLimit() {
        int limit = dynamicTenantLimit;
        if (limit == 0) {
            // No dynamic tenant limit, nothing to do
            return;
        }
        int toEvict = dynamicTenantsConfig.size() - limit;
        if (toEvict <= 0) {
            // Nothing to evict
            return;
        }
        if (!tenantEvictionRunning.compareAndSet(false, true)) {
            // Eviction running in another thread, don't start a concurrent one.
            return;
        }
        try {
            do {
                EvictionCandidate[] candidates = new EvictionCandidate[toEvict];
                int numCandidates = 0;
                // Current max
                long maxLastUsed = Long.MAX_VALUE;

                // Collect the required number of tenants to evict by visiting each dynamic tenant
                for (Map.Entry<String, TenantConfigContext> e : dynamicTenantsConfig.entrySet()) {
                    TenantConfigContext c = e.getValue();
                    long lastUsed = c.lastUsed;
                    if (lastUsed >= maxLastUsed) {
                        // Tenant is too young, skip
                        continue;
                    }

                    // Found a candidate with a lastUsed less than the current oldest
                    EvictionCandidate evictionCandidate = new EvictionCandidate(e.getKey(), c, lastUsed);
                    if (numCandidates < toEvict) {
                        // Collect until we hit the number of tenants to evict
                        candidates[numCandidates++] = evictionCandidate;
                        if (numCandidates == toEvict) {
                            // Calculate the new max lastUsed from the list of eviction candidates
                            maxLastUsed = evictionCandidatesMaxLastUsed(candidates);
                        }
                    } else {
                        // Replace the current newest eviction candidate with the current candidate
                        for (int i = 0; i < numCandidates; i++) {
                            if (candidates[i].lastUsed == maxLastUsed) {
                                candidates[i] = evictionCandidate;
                                break;
                            }
                        }
                        // Recalculate the max lastUsed
                        maxLastUsed = evictionCandidatesMaxLastUsed(candidates);
                    }
                }

                // Evict the tenants that haven't been used since eviction started
                for (EvictionCandidate candidate : candidates) {
                    // Only evict the tenant, if it hasn't been used since eviction started
                    evictTenant(candidate);
                }

                toEvict = dynamicTenantsConfig.size() - limit;
            } while (toEvict > 0);
        } finally {
            tenantEvictionRunning.set(false);
        }
    }

    // VisibleForTesting
    boolean evictTenant(EvictionCandidate candidate) {
        if (candidate != null && candidate.lastUsed == candidate.context.lastUsed) {
            dynamicTenantsConfig.remove(candidate.tenantId);
            destroyContext(candidate.context);
            return true;
        }
        return false;
    }

    private static long evictionCandidatesMaxLastUsed(EvictionCandidate[] candidates) {
        long max = 0L;
        for (EvictionCandidate candidate : candidates) {
            max = Math.max(max, candidate.lastUsed);
        }
        return max;
    }
}
