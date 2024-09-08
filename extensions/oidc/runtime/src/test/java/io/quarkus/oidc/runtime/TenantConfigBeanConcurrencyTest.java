package io.quarkus.oidc.runtime;

import static io.quarkus.oidc.runtime.TenantConfigBeanAsyncTest.tenantConfig;
import static io.quarkus.oidc.runtime.TenantConfigBeanAsyncTest.testContext;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import io.quarkus.oidc.OidcTenantConfig;
import io.smallrye.mutiny.Uni;

public class TenantConfigBeanConcurrencyTest {
    @Test
    public void concurrentCreatesSuccessDynamicTenant() throws Exception {
        // using "availableProcessors" is just an illusion...
        var concurrency = Runtime.getRuntime().availableProcessors();

        var enterLatch = new CountDownLatch(1);
        var createLatch = new CountDownLatch(1);
        var entered = new AtomicInteger();
        var tenantId = "my-tenant";

        var executor = Executors.newCachedThreadPool();
        try {

            var bean = new TenantConfigBean(Map.of(), testContext("Default"), System::nanoTime, 0,
                    (oidcTenantConfig, dynamicTenant, id) -> Uni.createFrom()
                            .completionStage(CompletableFuture.supplyAsync(() -> {
                                entered.incrementAndGet();
                                enterLatch.countDown();
                                try {
                                    createLatch.await();
                                } catch (InterruptedException e) {
                                    throw new RuntimeException(e);
                                }
                                return testReadyContext(id);
                            }, executor)));

            var unis = new ArrayList<Uni<TenantConfigContext>>();
            for (int i = 0; i < concurrency; i++) {
                unis.add(bean.getOrCreateTenantContext(tenantConfig(tenantId), true));
            }

            enterLatch.await();
            assertEquals(1, entered.get());

            assertNull(bean.getDynamicTenantConfigContext(tenantId));
            assertNull(bean.getDynamicTenantOidcConfig(tenantId));

            createLatch.countDown();

            TenantConfigContext context = null;
            OidcTenantConfig config = null;
            for (int i = 0; i < unis.size(); i++) {
                var uni = unis.get(i);
                TenantConfigContext ctx = uni.await().indefinitely();

                if (i == 0) {
                    context = bean.getDynamicTenantConfigContext(tenantId);
                    assertNotNull(context);
                    assertTrue(context.isReady());
                    config = bean.getDynamicTenantOidcConfig(tenantId);
                    assertNotNull(config);
                }

                assertSame(context, ctx);
                assertSame(config, ctx.oidcConfig);
            }

        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void concurrentCreatesSuccessStaticTenant() throws Exception {
        // using "availableProcessors" is just an illusion...
        var concurrency = Runtime.getRuntime().availableProcessors();

        var enterLatch = new CountDownLatch(1);
        var createLatch = new CountDownLatch(1);
        var entered = new AtomicInteger();
        var tenantId = "my-tenant";

        var executor = Executors.newCachedThreadPool();
        try {

            var bean = new TenantConfigBean(Map.of(tenantId, testContext(tenantId)), testContext("Default"), System::nanoTime,
                    0,
                    (oidcTenantConfig, dynamicTenant, id) -> Uni.createFrom()
                            .completionStage(CompletableFuture.supplyAsync(() -> {
                                entered.incrementAndGet();
                                enterLatch.countDown();
                                try {
                                    createLatch.await();
                                } catch (InterruptedException e) {
                                    throw new RuntimeException(e);
                                }
                                return testReadyContext(id);
                            }, executor)));

            var unis = new ArrayList<Uni<TenantConfigContext>>();
            for (int i = 0; i < concurrency; i++) {
                unis.add(bean.getOrCreateTenantContext(tenantConfig(tenantId), false));
            }

            enterLatch.await();
            assertEquals(1, entered.get());

            assertNotNull(bean.getStaticTenantConfigContext(tenantId));
            assertFalse(bean.getStaticTenantConfigContext(tenantId).isReady());
            assertNotNull(bean.getStaticTenantOidcConfig(tenantId));

            createLatch.countDown();

            TenantConfigContext context = null;
            OidcTenantConfig config = null;
            for (int i = 0; i < unis.size(); i++) {
                var uni = unis.get(i);
                TenantConfigContext ctx = uni.await().indefinitely();

                if (i == 0) {
                    context = bean.getStaticTenantConfigContext(tenantId);
                    assertNotNull(context);
                    assertTrue(context.isReady());
                    config = bean.getStaticTenantOidcConfig(tenantId);
                    assertNotNull(config);
                }

                assertSame(context, ctx);
                assertSame(config, ctx.oidcConfig);
            }

        } finally {
            executor.shutdown();
        }
    }

    static TenantConfigContext testReadyContext(String tenantId) {
        OidcTenantConfig config = tenantConfig(tenantId);
        return new TenantConfigContext(new OidcProvider(null, null, null, null), config, Map.of());
    }
}
