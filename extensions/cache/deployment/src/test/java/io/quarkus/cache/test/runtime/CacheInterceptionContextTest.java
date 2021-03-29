package io.quarkus.cache.test.runtime;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.annotation.Annotation;
import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import io.quarkus.cache.CacheResult;
import io.quarkus.cache.runtime.CacheInterceptionContext;

public class CacheInterceptionContextTest {

    @Test
    public void testConstructor() {
        assertThrows(NullPointerException.class, () -> {
            new CacheInterceptionContext<>(null, new ArrayList<>());
        }, "A NullPointerException should be thrown when the interceptor bindings list is null");
        assertThrows(NullPointerException.class, () -> {
            new CacheInterceptionContext<>(new ArrayList<>(), null);
        }, "A NullPointerException should be thrown when the cache key parameter positions list is null");
        // Empty lists should be allowed.
        new CacheInterceptionContext<>(new ArrayList<>(), new ArrayList<>());
    }

    @Test
    public void testImmutability() {
        CacheInterceptionContext<CacheResult> context = new CacheInterceptionContext<>(new ArrayList<>(), new ArrayList<>());
        // Lists should be unmodifiable.
        assertThrows(UnsupportedOperationException.class, () -> {
            context.getInterceptorBindings().add(new CacheResult() {
                @Override
                public Class<? extends Annotation> annotationType() {
                    return CacheResult.class;
                }

                @Override
                public String cacheName() {
                    return "cacheName";
                }

                @Override
                public long lockTimeout() {
                    return 0;
                }
            });
        });
        assertThrows(UnsupportedOperationException.class, () -> {
            context.getCacheKeyParameterPositions().add((short) 123);
        });
    }
}
