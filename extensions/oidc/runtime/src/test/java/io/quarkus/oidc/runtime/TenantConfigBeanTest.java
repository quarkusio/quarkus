package io.quarkus.oidc.runtime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import io.quarkus.oidc.OidcTenantConfig;
import io.smallrye.mutiny.Uni;

public class TenantConfigBeanTest {

    /**
     * Straight forward dynamic-tenant limit test, create 10 tenants - implementation limits the number of dynamic tenants.
     */
    @Test
    public void dynamicTenantsLimit() {
        AtomicLong clock = new AtomicLong();

        int limit = 3;

        Set<String> evictedTenants = new HashSet<>();

        TenantConfigBean bean = new TenantConfigBean(Collections.emptyMap(), testContext("Default"), clock::get, limit,
                (oidcTenantConfig, dynamicTenant, tenantId) -> Uni.createFrom().item(testContext(tenantId))) {
            @Override
            boolean evictTenant(EvictionCandidate candidate) {
                boolean evicted = super.evictTenant(candidate);
                if (evicted) {
                    evictedTenants.add(candidate.tenantId());
                }
                return evicted;
            }
        };

        for (int i = 0; i < 10; i++) {
            clock.incrementAndGet();
            bean.getOrCreateTenantContext(tenantConfig("tenant-" + i), true).await().indefinitely();

            for (int i1 = 0; i1 < i - limit; i1++) {
                String tenantId = "tenant-" + i1;
                assertNull(bean.getDynamicTenantConfigContext(tenantId), tenantId);
                assertTrue(evictedTenants.contains(tenantId), tenantId);
            }
        }
    }

    /**
     * Simulate delayed eviction (already running).
     *
     * <ol>
     * <li>Create 10 tenants (no/blocked eviction).
     * <li>Unblock eviction.
     * <li>Create another tenant.
     * <li>First 8 created tenants must have been evicted.
     * </ol>
     */
    @Test
    public void dynamicTenantsLimitDelayedEviction() {
        AtomicLong clock = new AtomicLong();

        int limit = 3;

        Set<String> evictedTenants = new HashSet<>();

        TenantConfigBean bean = new TenantConfigBean(Collections.emptyMap(), testContext("Default"), clock::get, limit,
                (oidcTenantConfig, dynamicTenant, tenantId) -> Uni.createFrom().item(testContext(tenantId))) {
            @Override
            boolean evictTenant(EvictionCandidate candidate) {
                boolean evicted = super.evictTenant(candidate);
                if (evicted) {
                    evictedTenants.add(candidate.tenantId());
                }
                return evicted;
            }
        };

        bean.tenantEvictionRunning.set(true);

        for (int i = 0; i < 10; i++) {
            clock.incrementAndGet();
            String tenantId = "tenant-" + i;
            bean.getOrCreateTenantContext(tenantConfig(tenantId), true).await().indefinitely();
            assertTrue(evictedTenants.isEmpty(), tenantId);
        }

        bean.tenantEvictionRunning.set(false);

        clock.incrementAndGet();
        bean.getOrCreateTenantContext(tenantConfig("tenant-X"), true).await().indefinitely();
        for (int i = 0; i < 8; i++) {
            String tenantId = "tenant-" + i;
            assertNull(bean.getDynamicTenantConfigContext(tenantId), tenantId);
            assertTrue(evictedTenants.contains(tenantId), tenantId);
        }
    }

    /**
     * Simulate delayed eviction (already running).
     *
     * <ol>
     * <li>Create 10 tenants (no/blocked eviction).
     * <li>Simulate usa of the first 5 tenants.
     * <li>Unblock eviction.
     * <li>Create another tenant.
     * <li>Tenants 0-2 + 5-9 must have been evicted (3,4,X are the three newest).
     * </ol>
     */
    @Test
    public void dynamicTenantsLimitDelayedEvictionRecentlyUsed() {
        AtomicLong clock = new AtomicLong();

        int limit = 3;

        Set<String> evictedTenants = new HashSet<>();

        TenantConfigBean bean = new TenantConfigBean(Collections.emptyMap(), testContext("Default"), clock::get, limit,
                (oidcTenantConfig, dynamicTenant, tenantId) -> Uni.createFrom().item(testContext(tenantId))) {
            @Override
            boolean evictTenant(EvictionCandidate candidate) {
                boolean evicted = super.evictTenant(candidate);
                if (evicted) {
                    evictedTenants.add(candidate.tenantId());
                }
                return evicted;
            }
        };

        bean.tenantEvictionRunning.set(true);

        for (int i = 0; i < 10; i++) {
            clock.incrementAndGet();
            String tenantId = "tenant-" + i;
            bean.getOrCreateTenantContext(tenantConfig(tenantId), true).await().indefinitely();
            assertTrue(evictedTenants.isEmpty(), tenantId);
        }

        for (int i = 0; i < 5; i++) {
            clock.incrementAndGet();
            bean.getDynamicTenantConfigContext("tenant-" + i);
        }

        bean.tenantEvictionRunning.set(false);

        clock.incrementAndGet();
        bean.getOrCreateTenantContext(tenantConfig("tenant-X"), true).await().indefinitely();

        Stream.of(0, 1, 2, 5, 6, 7, 8, 9).forEach(i -> {
            String tenantId = "tenant-" + i;
            assertNull(bean.getDynamicTenantConfigContext(tenantId), tenantId);
            assertTrue(evictedTenants.contains(tenantId), tenantId);
        });
        {
            String tenantId = "tenant-X";
            assertNotNull(bean.getDynamicTenantConfigContext(tenantId), tenantId);
            assertFalse(evictedTenants.contains(tenantId), tenantId);
        }
    }

    /**
     * Simulate delayed eviction (already running).
     *
     * <ol>
     * <li>Create 10 tenants (no/blocked eviction).
     * <li>Unblock eviction.
     * <li>Create another tenant.
     * <li>Simulate use of the first 5 tenants _during_ eviction.
     * <li>Tenants 0-2 + 5-9 must have been evicted (3,4,X are the three newest).
     * </ol>
     */
    @Test
    public void dynamicTenantsLimitDelayedEvictionConcurrentAccess() {
        AtomicLong clock = new AtomicLong();

        int limit = 3;

        Set<String> evictedTenants = new HashSet<>();
        Map<String, Long> newMaxLastUsed = new HashMap<>();

        TenantConfigBean bean = new TenantConfigBean(Collections.emptyMap(), testContext("Default"), clock::get, limit,
                (oidcTenantConfig, dynamicTenant, tenantId) -> Uni.createFrom().item(testContext(tenantId))) {
            @Override
            boolean evictTenant(EvictionCandidate candidate) {
                Long newLastUsed = newMaxLastUsed.get(candidate.tenantId());
                if (newLastUsed != null) {
                    candidate.context().lastUsed = newLastUsed;
                }
                boolean evicted = super.evictTenant(candidate);
                if (evicted) {
                    evictedTenants.add(candidate.tenantId());
                }
                return evicted;
            }
        };

        bean.tenantEvictionRunning.set(true);

        for (int i = 0; i < 10; i++) {
            clock.incrementAndGet();
            String tenantId = "tenant-" + i;
            bean.getOrCreateTenantContext(tenantConfig(tenantId), true).await().indefinitely();
            assertTrue(evictedTenants.isEmpty(), tenantId);
        }

        bean.tenantEvictionRunning.set(false);

        for (int i = 0; i < 5; i++) {
            newMaxLastUsed.put("tenant-" + i, clock.incrementAndGet());
        }

        clock.incrementAndGet();
        bean.getOrCreateTenantContext(tenantConfig("tenant-X"), true).await().indefinitely();

        Stream.of(0, 1, 2, 5, 6, 7, 8, 9).forEach(i -> {
            String tenantId = "tenant-" + i;
            assertNull(bean.getDynamicTenantConfigContext(tenantId), tenantId);
            assertTrue(evictedTenants.contains(tenantId), tenantId);
        });
        {
            String tenantId = "tenant-X";
            assertNotNull(bean.getDynamicTenantConfigContext(tenantId), tenantId);
            assertFalse(evictedTenants.contains(tenantId), tenantId);
        }
    }

    static TenantConfigContext testContext(String tenantId) {
        OidcTenantConfig config = tenantConfig(tenantId);
        return new TenantConfigContext(null, config, Collections.emptyMap());
    }

    static OidcTenantConfig tenantConfig(String tenantId) {
        OidcTenantConfig config = new OidcTenantConfig();
        config.setTenantId(tenantId);
        return config;
    }
}
