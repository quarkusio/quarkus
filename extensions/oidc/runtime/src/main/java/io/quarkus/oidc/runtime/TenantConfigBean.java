package io.quarkus.oidc.runtime;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.LongSupplier;

import jakarta.enterprise.context.spi.CreationalContext;

import org.jboss.logging.Logger;

import io.quarkus.arc.BeanDestroyer;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.common.runtime.OidcCommonUtils;
import io.quarkus.tls.TlsConfiguration;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;

public class TenantConfigBean {
    /*
     * Note: this class is publicly documented on https://quarkus.io/guides/security-oidc-code-flow-authentication.
     */

    private static final Logger LOG = Logger.getLogger(TenantConfigBean.class);

    private final TenantsMap staticTenantsConfig;
    private final TenantsMap dynamicTenantsConfig;
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
        this.staticTenantsConfig = new TenantsMap(false, false, staticTenantsConfig);
        this.dynamicTenantsConfig = new TenantsMap(true, dynamicTenantLimit > 0, Map.of());
        this.clock = clock;
        this.dynamicTenantLimit = dynamicTenantLimit;
        this.defaultTenant = defaultTenant;
        this.tenantContextFactory = tenantContextFactory;
    }

    Uni<TenantConfigContext> getOrCreateTenantContext(OidcTenantConfig oidcConfig, boolean dynamicTenant) {
        var tenantId = oidcConfig.getTenantId().orElseThrow();
        var tenants = dynamicTenant ? dynamicTenantsConfig : staticTenantsConfig;

        // Fast-path, no volatile reads
        var context = tenants.fastGet(tenantId);
        if (context != null && context.isReady()) {
            return Uni.createFrom().item(context);
        }

        return tenants.slowGetOrCreateReady(tenantId, oidcConfig);
    }

    /**
     * Returns a static tenant's config context or {@code null}, if the tenant does not exist.
     */
    public TenantConfigContext getStaticTenantConfigContext(String tenantId) {
        return staticTenantsConfig.fastGet(tenantId);
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
        return staticTenantsConfig.tenantsCopy();
    }

    /**
     * Returns a dynamic tenant's config context or {@code null}, if the tenant does not exist.
     */
    public TenantConfigContext getDynamicTenantConfigContext(String tenantId) {
        return dynamicTenantsConfig.fastGet(tenantId);
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
        return dynamicTenantsConfig.tenantsCopy();
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
            for (var i : instance.staticTenantsConfig.holders()) {
                destroyContext(i.context());
            }
            for (var i : instance.dynamicTenantsConfig.holders()) {
                destroyContext(i.context());
            }
        }
    }

    record EvictionCandidate(String tenantId, TenantHolder holder, long lastUsed) {
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
                // Note: `dynamicTenantsConfig.entrySet()` creates a copy of the "live" map while holding the
                // tenants-map lock.
                Set<Map.Entry<String, TenantHolder>> dynamicTenants = dynamicTenantsConfig.entrySet();

                // Re-calculate the number of tenants to evict - the value might have changed since the start
                // of the `enforceDynamicTenantLimit()` function or since the last check at the end of the
                // do-while loop.
                toEvict = dynamicTenants.size() - limit;
                if (toEvict <= 0) {
                    // Nothing to evict
                    return;
                }

                EvictionCandidate[] candidates = new EvictionCandidate[toEvict];
                int numCandidates = 0;
                // Current max
                long maxLastUsed = Long.MAX_VALUE;

                // Collect the required number of tenants to evict by visiting each dynamic tenant.
                for (Map.Entry<String, TenantHolder> e : dynamicTenants) {
                    var holder = e.getValue();
                    long lastUsed = holder.lastUsed;
                    if (lastUsed >= maxLastUsed) {
                        // Tenant is too young, skip
                        continue;
                    }

                    // Found a candidate with a lastUsed less than the current oldest
                    EvictionCandidate evictionCandidate = new EvictionCandidate(e.getKey(), holder, lastUsed);
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

                // Check if there's more to do...
                toEvict = dynamicTenantsConfig.size() - limit;
            } while (toEvict > 0);
        } finally {
            tenantEvictionRunning.set(false);
        }
    }

    // VisibleForTesting
    boolean evictTenant(EvictionCandidate candidate) {
        // Note: candidate.holder is the "live" holder object, not a copy, so `lastUsed` is the real value.
        // There is still a very unlikely (but technically possible) race condition that the tenant was accessed
        // before it could be removed from the map _and_ the change to the map became visible.
        if (candidate != null && candidate.lastUsed == candidate.holder.lastUsed) {
            var context = candidate.holder.context();
            // Note: the `.remove()` operates while holding the tenants-map lock.
            dynamicTenantsConfig.remove(candidate.tenantId);
            destroyContext(context);
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

    // Tenant contexts are managed via the following map-container (`TenantsMap`) and map-value type (`TenantHolder`),
    // both are optimized for the "99.99% case" that the maps of static and dynamic tenants are "fully populated" and
    // all (active) tenants have a "ready" context.
    //
    // Retrieving a tenant hits the "fast path" in the vast majority of all invocations, accessing only non-volatile
    // fields and a non-concurrent hash map.
    //
    // In case a tenant is not yet present (new dynamic tenant ID or late-initialized static default tenant), the
    // implementations switches to the "slow path" and creates a new hash map with the new tenant and updates the
    // non-volatile field referencing the tenants-map. Other threads that might not yet "see" the updated tenants-map
    // field would also enter the slow path - but once the slow path has been executed, the update to the tenants-map
    // field will be "visible".
    //
    // The `TenantHolder` uses a mixture of non-volatile (the "99.99% case" is accessing a ready context) and
    // volatile fields. The latter are used to make changes immediately "visible" to other threads - the order of
    // writes is important here. Unlike `TenantsMap`, the `TenantHolder` class does not use/need a lock object or
    // monitor.

    /**
     * Manages tenants, optimized for non-volatile reads for performance reasons.
     */
    private final class TenantsMap {
        private final boolean dynamicTenants;
        private final boolean needsClock;
        private final Lock lock = new ReentrantLock();

        // Contains non-concurrent `java.util.HashMap` to reduce the amount of volatile read.
        private Map<String, TenantHolder> tenants;

        TenantsMap(boolean dynamicTenants, boolean needsClock, Map<String, TenantConfigContext> source) {
            this.dynamicTenants = dynamicTenants;
            this.needsClock = needsClock;
            this.tenants = new HashMap<>(source.size() * 4 / 3 + 1);
            for (Map.Entry<String, TenantConfigContext> e : source.entrySet()) {
                var context = e.getValue();
                var holder = new TenantHolder(context);
                tenants.put(e.getKey(), holder);
            }
        }

        /**
         * "Fast get" a tenant. "Fast" means that the implementation does not touch {@code volatile}s (except for
         * {@link TenantHolder#lastUsed}, if needed).
         */
        TenantConfigContext fastGet(String tenantId) {
            var holder = tenants.get(tenantId);
            LOG.tracef("fast get-tenant %s, exists: %s, has context: %s", tenantId, holder,
                    holder != null && holder.context() != null);
            if (holder != null) {
                if (needsClock) {
                    holder.setLastUsed(clock.getAsLong());
                }
                return holder.context();
            }
            return null;
        }

        /**
         * "Slow get or create" a tenant. "Slow" means that the function runs with {@link #lock} held, the
         * implementation does not block for a long time.
         *
         * <p>
         * TODO:
         * <ul>
         * <li>Check
         * {@link io.quarkus.oidc.runtime.OidcRecorder#createTenantContext(Vertx, OidcTenantConfig, boolean, String, TlsConfiguration)}
         * <li>{@link OidcCommonUtils#verifyEndpointUrl(String)} triggers DNS round trips!
         * <li>{@link OidcRecorder#createOidcClientUni(OidcTenantConfig, Vertx, TlsConfiguration)} might block</li>
         * </ul>
         */
        Uni<TenantConfigContext> slowGetOrCreateReady(String tenantId, OidcTenantConfig oidcConfig) {
            // Locks make changes visible
            lock.lock();
            try {
                // Lookup the tenant holder
                var holder = tenants.get(tenantId);
                LOG.tracef("slow get-tenant %s, exists: %s", tenantId, holder);
                if (holder != null) {
                    var context = holder.context();
                    if (context != null && context.isReady()) {
                        // Tenant holder exists and is ready - this branch is entered when the current thead could not "see" the update to either the `tenants` map or the update of the fields in `TenantHolder` before.
                        LOG.tracef("slow get-tenant %s, returning ready context", tenantId);
                        if (needsClock) {
                            holder.setLastUsed(clock.getAsLong());
                        }
                        return Uni.createFrom().item(context);
                    }

                    var future = holder.ctxFuture;
                    if (future == null) {
                        // "Nothing" is trying to create the ready context yet, start creation and provide a future to which concurrent creation-requests can subscribe to.
                        holder.ctxFuture = new CompletableFuture<>();
                        LOG.tracef("slow get-tenant %s, start creation (existing tenant)", tenantId);
                        return create(tenantId, oidcConfig, holder, holder);
                    }

                    // Another thread already created the future, subscribe to it.
                    LOG.tracef("slow get-tenant %s, returning Uni from future", tenantId);
                    return Uni.createFrom().future(future);
                }

                // Tenant is not in the tenants-map, add it with a future to which concurrent creation-requests can subscribe to and start creation.
                var newTenants = new HashMap<>(tenants);
                var newHolder = new TenantHolder(null);
                newHolder.ctxFuture = new CompletableFuture<>();
                if (needsClock) {
                    newHolder.setLastUsed(clock.getAsLong());
                }
                newTenants.put(tenantId, newHolder);
                this.tenants = newTenants;

                LOG.tracef("slow get-tenant %s, start creation (new tenant)", tenantId);
                var creation = create(tenantId, oidcConfig, null, newHolder);

                LOG.tracef("slow get-tenant %s, returning Uni from creation", tenantId);
                return creation;
            } finally {
                lock.unlock();
            }
        }

        void remove(String tenantId) {
            // Locks make changes visible
            lock.lock();
            try {
                tenants.remove(tenantId);
            } finally {
                lock.unlock();
            }
        }

        private Uni<TenantConfigContext> create(String tenantId, OidcTenantConfig oidcConfig, TenantHolder previous,
                TenantHolder holder) {
            Uni<TenantConfigContext> creation = tenantContextFactory.create(oidcConfig, dynamicTenants, tenantId).onItem()
                    .transform(
                            new Function<>() {
                                @Override
                                public TenantConfigContext apply(TenantConfigContext t) {
                                    LOG.debugf("Updating %s %s tenant config for %s", dynamicTenants ? "dynamic" : "static",
                                            t.isReady() ? "ready" : "not-ready", tenantId);

                                    holder.setContext(t);
                                    if (needsClock) {
                                        holder.setLastUsed(clock.getAsLong());
                                    }

                                    if (previous != null) {
                                        // Concurrent calls to createTenantContext may race, better "destroy" the previous
                                        // provider, if there's one.
                                        destroyContext(previous.context());
                                    } else if (dynamicTenants) {
                                        enforceDynamicTenantLimit();
                                    }
                                    return t;
                                }
                            });
            creation = creation.onFailure().invoke(new Consumer<Throwable>() {
                @Override
                public void accept(Throwable t) {
                    holder.failed(t);
                }
            });
            return creation;
        }

        Map<String, TenantConfigContext> tenantsCopy() {
            Map<String, TenantHolder> map;
            // Locks make changes visible
            lock.lock();
            try {
                map = tenants;
            } finally {
                lock.unlock();
            }
            Map<String, TenantConfigContext> copy = new HashMap<>(map.size() * 4 / 3 + 1);
            for (Map.Entry<String, TenantHolder> e : map.entrySet()) {
                copy.put(e.getKey(), e.getValue().context());
            }
            return copy;
        }

        Collection<TenantHolder> holders() {
            // Locks make changes visible
            lock.lock();
            try {
                return tenants.values();
            } finally {
                lock.unlock();
            }
        }

        int size() {
            // Locks make changes visible
            lock.lock();
            try {
                return tenants.size();
            } finally {
                lock.unlock();
            }
        }

        public Set<Map.Entry<String, TenantHolder>> entrySet() {
            // Locks make changes visible
            lock.lock();
            try {
                return tenants.entrySet();
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * Holds the {@link TenantConfigContext} for a tenant, optimized for non-volatile reads for "ready" tenants.
     */
    // VisibleForTesting
    static final class TenantHolder {
        /**
         * Only populated when the context is ready.
         */
        TenantConfigContext readyContext;

        private volatile CompletableFuture<TenantConfigContext> ctxFuture;
        private volatile TenantConfigContext context;
        private volatile long lastUsed;

        TenantHolder(TenantConfigContext context) {
            if (context != null) {
                if (context.isReady()) {
                    this.readyContext = context;
                } else {
                    this.context = context;
                }
            }
        }

        TenantConfigContext context() {
            // Fast path, no volatile read
            var c = readyContext;
            if (c != null) {
                return c;
            }

            // Slow path, volatile read
            return context;
        }

        void setLastUsed(long lastUsed) {
            this.lastUsed = lastUsed;
        }

        /**
         * Called when tenant-creation returned a context.
         *
         * <p>
         * Completes the completable-future to which concurrent creation-requests might have subscribed to.
         *
         * <p>
         * If {@code ctx} is ready, directly populate it into the non-{@code volatile} {@link #readyContext} field,
         * otherwise into the {@code volatile} {@link #context} field.
         *
         * <p>
         * Clears the {@link #ctxFuture} field, completes the future.
         */
        void setContext(TenantConfigContext ctx) {
            if (ctx != null) {
                // Read `ctxFuture` _before_ updating the context fields.
                var future = this.ctxFuture;
                if (ctx.isReady()) {
                    // non-volatile write
                    this.readyContext = ctx;
                    // volatile write ("publishes" the non-volatile write to readyContext)
                    // Also setting a "ready" to this field, so that a call to `#context()` can read the updated
                    // state "immediately".
                    this.context = ctx;
                } else {
                    // It is rather not the case that this if-branch would ever be executed, but it does not hurt to
                    // handle the not-ready case here.

                    // non-volatile write
                    this.readyContext = null;
                    // volatile write ("publishes" the non-volatile write to readyContext)
                    this.context = ctx;
                }
                if (future != null) {
                    // Reset `ctxFuture` as the last step, after updating the other fields above.
                    // At worst, a racing execution of `slowGetOrCreateReady` would retrieve the same result,
                    // which is not different from the handling of a concurrent execution.
                    this.ctxFuture = null;
                    if (!future.isDone()) {
                        // Propagate the result
                        future.complete(ctx);
                    }
                }
            }
        }

        /**
         * Called when tenant-creation failed, for example when the OIDC server is (still) not reachable.
         *
         * <p>
         * Clears {@link #ctxFuture} and propagates the failure to concurrent creation-requests via that future.
         *
         * <p>
         * Once the {@link #ctxFuture} is cleared, a following tenant-create request causes an immediate retry.
         * We could memoize the failure for some time and delay the next creation-request to throttle creation attempts
         * against an unreachable/down OIDC service.
         */
        void failed(Throwable t) {
            var future = this.ctxFuture;
            if (future != null) {
                this.ctxFuture = null;
                if (!future.isDone()) {
                    // Propagate the failure
                    future.completeExceptionally(t);
                }
            }
        }
    }
}
