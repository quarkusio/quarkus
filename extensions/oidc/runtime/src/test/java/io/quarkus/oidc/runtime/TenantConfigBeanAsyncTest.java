package io.quarkus.oidc.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.oidc.OidcTenantConfig;
import io.smallrye.mutiny.Uni;

public class TenantConfigBeanAsyncTest {

    private static ExecutorService executor;
    private Semaphore asyncCompletion;
    private AtomicInteger started;

    @BeforeAll
    static void beforeAll() {
        executor = Executors.newCachedThreadPool();
    }

    @AfterAll
    static void afterAll() {
        executor.shutdown();
    }

    @BeforeEach
    void beforeEach() {
        started = new AtomicInteger();
        asyncCompletion = new Semaphore(0);
    }

    Uni<TenantConfigContext> asyncCreate(String tenantId) {
        return Uni.createFrom().completionStage(CompletableFuture.supplyAsync(() -> {
            try {
                started.incrementAndGet();
                asyncCompletion.acquire();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return testContext(tenantId);
        }, executor));
    }

    @Test
    public void dynamicTenantsLimitAsync() {
        AtomicLong clock = new AtomicLong();

        int limit = 3;

        Set<String> evictedTenants = new HashSet<>();

        TenantConfigBean bean = new TenantConfigBean(Collections.emptyMap(), testContext("Default"), clock::get, limit,
                (oidcTenantConfig, dynamicTenant, tenantId) -> asyncCreate(tenantId)) {
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
            Uni<TenantConfigContext> uni = bean.getOrCreateTenantContext(tenantConfig("tenant-" + i), true);

            asyncCompletion.release();
            uni.await().indefinitely();

            for (int i1 = 0; i1 < i - limit; i1++) {
                String tenantId = "tenant-" + i1;
                assertNull(bean.getDynamicTenantConfigContext(tenantId), tenantId);
                assertTrue(evictedTenants.contains(tenantId), tenantId);
            }
        }

        assertEquals(10, started.get());
    }

    @Test
    public void dynamicTenantsLimitDelayedEvictionAsync() {
        AtomicLong clock = new AtomicLong();

        int limit = 3;

        TenantConfigBean bean = new TenantConfigBean(Collections.emptyMap(), testContext("Default"), clock::get, limit,
                (oidcTenantConfig, dynamicTenant, tenantId) -> asyncCreate(tenantId));

        bean.tenantEvictionRunning.set(true);

        List<Uni<TenantConfigContext>> unis = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            clock.incrementAndGet();
            String tenantId = "tenant-" + i;
            unis.add(bean.getOrCreateTenantContext(tenantConfig(tenantId), true));
            assertEquals(i + 1, bean.getDynamicTenantsConfig().size());
        }

        bean.tenantEvictionRunning.set(false);

        clock.incrementAndGet();
        unis.add(bean.getOrCreateTenantContext(tenantConfig("tenant-X"), true));

        asyncCompletion.release(11);
        Uni.combine().all().unis(unis).with(l -> l).await().indefinitely();
        assertEquals(11, started.get());

        assertEquals(3, bean.getDynamicTenantsConfig().size());
    }

    @Test
    public void dynamicTenantsLimitDelayedEvictionConcurrentAccessAsync() {
        AtomicLong clock = new AtomicLong();

        int limit = 3;

        AtomicInteger concurrentAccessRemaining = new AtomicInteger(5);

        TenantConfigBean bean = new TenantConfigBean(Collections.emptyMap(), testContext("Default"), clock::get, limit,
                (oidcTenantConfig, dynamicTenant, tenantId) -> asyncCreate(tenantId)) {
            @Override
            boolean evictTenant(EvictionCandidate candidate) {
                if (concurrentAccessRemaining.decrementAndGet() >= 0) {
                    candidate.holder().setLastUsed(clock.incrementAndGet());
                }
                return super.evictTenant(candidate);
            }
        };

        bean.tenantEvictionRunning.set(true);

        List<Uni<TenantConfigContext>> unis = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            clock.incrementAndGet();
            unis.add(bean.getOrCreateTenantContext(tenantConfig("tenant-" + i), true));
            assertEquals(i + 1, bean.getDynamicTenantsConfig().size());
        }

        bean.tenantEvictionRunning.set(false);

        clock.incrementAndGet();
        unis.add(bean.getOrCreateTenantContext(tenantConfig("tenant-X"), true));

        asyncCompletion.release(11);
        Uni.combine().all().unis(unis).with(l -> l).await().indefinitely();
        assertEquals(11, started.get());

        assertEquals(3, bean.getDynamicTenantsConfig().size());
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
