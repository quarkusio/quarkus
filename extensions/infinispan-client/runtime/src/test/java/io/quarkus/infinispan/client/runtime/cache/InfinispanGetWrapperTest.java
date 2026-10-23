package io.quarkus.infinispan.client.runtime.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.infinispan.client.hotrod.RemoteCache;
import org.junit.jupiter.api.Test;

/**
 * Plain unit test (no Infinispan server) for {@link InfinispanGetWrapper}.
 */
public class InfinispanGetWrapperTest {

    @Test
    public void testValueLoaderFailureIsPropagatedAndNotCached() {
        Map<Object, CompletableFuture<Object>> synchronousGets = new ConcurrentHashMap<>();
        InfinispanGetWrapper wrapper = new InfinispanGetWrapper(emptyRemoteCache(false), synchronousGets);
        AtomicInteger loads = new AtomicInteger();

        for (int i = 0; i < 2; i++) {
            assertThatThrownBy(() -> wrapper.get("key", key -> {
                loads.incrementAndGet();
                throw new IllegalStateException("boom");
            }).await().atMost(Duration.ofSeconds(5)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("boom");
        }
        assertThat(loads).hasValue(2);
        assertThat(synchronousGets).isEmpty();
    }

    @Test
    public void testValueLoaderFailureIsNotSharedWithRetries() {
        Map<Object, CompletableFuture<Object>> synchronousGets = new ConcurrentHashMap<>();
        InfinispanGetWrapper wrapper = new InfinispanGetWrapper(emptyRemoteCache(true), synchronousGets);
        AtomicInteger loads = new AtomicInteger();

        assertThatThrownBy(() -> wrapper.get("key", key -> {
            loads.incrementAndGet();
            throw new IllegalStateException("boom");
        }).onFailure().retry().atMost(3).await().atMost(Duration.ofSeconds(5)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("boom");
        assertThat(loads).hasValue(4);
        assertThat(synchronousGets).isEmpty();
    }

    /**
     * A remote cache that never contains the requested key, replying synchronously or from another thread.
     */
    private static RemoteCache<?, ?> emptyRemoteCache(boolean async) {
        return (RemoteCache<?, ?>) Proxy.newProxyInstance(InfinispanGetWrapperTest.class.getClassLoader(),
                new Class<?>[] { RemoteCache.class }, (proxy, method, args) -> {
                    if (method.getName().equals("getAsync")) {
                        return async ? CompletableFuture.supplyAsync(() -> null) : CompletableFuture.completedFuture(null);
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }
}
