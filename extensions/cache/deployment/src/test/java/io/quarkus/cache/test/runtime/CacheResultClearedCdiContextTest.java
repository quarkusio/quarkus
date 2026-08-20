package io.quarkus.cache.test.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.control.RequestContextController;
import jakarta.inject.Inject;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.context.ThreadContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.Version;
import io.quarkus.cache.CacheManager;
import io.quarkus.cache.CacheResult;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.context.api.ManagedExecutorConfig;
import io.smallrye.context.api.NamedInstance;

/**
 * A ManagedExecutor with CDI cleared is used so the HTTP request scope can end while work
 * continues. The worker then activates a RequestContext, sets request-scoped data, and calls
 * a {@code @CacheResult} method. That data must still be visible inside the cached method —
 * the synchronous {@code @CacheResult} path must not go through Mutiny {@code Uni.await()},
 * which would let SmallRye Context Propagation re-clear CDI.
 */
public class CacheResultClearedCdiContextTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-smallrye-context-propagation-deployment",
                            Version.getVersion())))
            .withApplicationRoot(jar -> jar.addClasses(
                    Dispatcher.class,
                    CachedLookup.class,
                    RequestData.class));

    @Inject
    Dispatcher dispatcher;

    @Inject
    CacheManager cacheManager;

    @Test
    void requestScopedDataVisibleInsideCacheResultWhenCdiClearedOnManagedExecutor() throws Exception {
        cacheManager.getCache("issue-52119-cache").ifPresent(cache -> cache.invalidateAll().await().indefinitely());

        String withoutCache = dispatcher.lookupWithoutCache().get(5, TimeUnit.SECONDS);
        assertEquals("expected", withoutCache,
                "baseline: manual RequestContext + cleared CDI without @CacheResult");

        String withCache = dispatcher.lookupWithCache().get(5, TimeUnit.SECONDS);
        assertEquals("expected", withCache,
                "@CacheResult must see RequestScoped data set on the worker after CDI was cleared");
    }

    @ApplicationScoped
    static class Dispatcher {

        @Inject
        @ManagedExecutorConfig(propagated = ThreadContext.ALL_REMAINING, cleared = ThreadContext.CDI)
        @NamedInstance("cleared-cdi")
        ManagedExecutor clearedCdiExecutor;

        @Inject
        RequestContextController requestContextController;

        @Inject
        RequestData requestData;

        @Inject
        CachedLookup cachedLookup;

        CompletableFuture<String> lookupWithoutCache() {
            return clearedCdiExecutor.supplyAsync(() -> {
                requestContextController.activate();
                try {
                    requestData.setValue("expected");
                    return cachedLookup.lookup();
                } finally {
                    requestData.setValue(null);
                    requestContextController.deactivate();
                }
            });
        }

        CompletableFuture<String> lookupWithCache() {
            return clearedCdiExecutor.supplyAsync(() -> {
                requestContextController.activate();
                try {
                    requestData.setValue("expected");
                    return cachedLookup.lookupFromCache();
                } finally {
                    requestData.setValue(null);
                    requestContextController.deactivate();
                }
            });
        }
    }

    @ApplicationScoped
    static class CachedLookup {

        @Inject
        RequestData requestData;

        String lookup() {
            return requestData.getValue();
        }

        @CacheResult(cacheName = "issue-52119-cache")
        String lookupFromCache() {
            return requestData.getValue();
        }
    }

    @RequestScoped
    static class RequestData {

        private String value;

        String getValue() {
            return value;
        }

        void setValue(String value) {
            this.value = value;
        }
    }
}
