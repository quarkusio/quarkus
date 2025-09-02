package io.quarkus.cache.test.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheKeyGenerator;
import io.quarkus.cache.CacheManager;
import io.quarkus.cache.CachedResults;
import io.quarkus.cache.CaffeineCache;
import io.quarkus.test.QuarkusUnitTest;

public class CachedResultsTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(
                    jar -> jar.addClasses(EmbeddingModel.class, EmbeddingModelImpl.class, AnotherModel.class));

    @Inject
    @CachedResults(cacheName = "foo")
    EmbeddingModel model1;

    @Inject
    @CachedResults(cacheName = "foo")
    EmbeddingModel model2;

    @Inject
    @CachedResults(cacheName = "baz", exclude = "bar")
    EmbeddingModel model3;

    @Inject
    @CachedResults
    EmbeddingModel model4;

    @Inject
    @CachedResults
    EmbeddingModel model5;

    @Inject
    @CachedResults(cacheName = "model6", keyGenerator = CustomKeyGenerator.class)
    EmbeddingModel model6;

    @CachedResults
    AnotherModel anotherModel1;

    @Inject
    CacheManager cacheManager;

    @Test
    public void testCached() {
        int ret1 = model1.foo(1);
        assertEquals(ret1, model1.foo(1));
        // model2 shares the same cache
        assertEquals(ret1, model2.foo(1));
        // model3 has a different cache
        assertNotEquals(ret1, model3.foo(1));

        int ret2 = model1.foo(2);
        assertNotEquals(ret2, ret1);
        assertEquals(ret2, model1.foo(2));

        String ret3 = model1.bar();
        assertEquals(ret3, model1.bar());
        assertEquals(ret3, model1.bar());

        // model4 and model5 use a derived cache name
        int ret4 = model4.foo(10);
        assertEquals(ret4, model4.foo(10));
        assertEquals(ret4, model5.foo(10));
        assertNotEquals(ret1, model3.foo(10));
        assertTrue(
                cacheManager.getCache("io.quarkus.cache.test.runtime.CachedResultsTest$EmbeddingModel#foo(int)").isPresent(),
                cacheManager.getCacheNames().toString());

        // bar is ignored for model3
        String ret5 = model3.bar();
        assertNotEquals(ret5, model3.bar());

        // update() should never be cached
        model1.update();
        assertTrue(EmbeddingModelImpl.lastUpdate.get() > 0);

        // cache key is the method name
        assertEquals(model6.foo(12), model6.foo(21));
        assertEquals(model6.bar(), model6.bar());
        Optional<Cache> model6Cache = cacheManager.getCache("model6");
        assertTrue(model6Cache.isPresent());
        Set<Object> keys = model6Cache.get().as(CaffeineCache.class).keySet();
        assertTrue(keys.contains("foo"));
        assertTrue(keys.contains("bar"));

        // injected class has a no-args constructor
        assertEquals(anotherModel1.foo(111), anotherModel1.foo(111));
        assertEquals(anotherModel1.bar(), anotherModel1.bar());
        Class<?> clazz = anotherModel1.getClass();
        try {
            clazz.getDeclaredMethod("baz");
            fail();
        } catch (NoSuchMethodException expected) {
        }
    }

    interface EmbeddingModel {

        // excluded
        void update();

        int foo(int val);

        String bar();

    }

    @Dependent
    static class AnotherModel {

        static final AtomicLong lastUpdate = new AtomicLong();

        int foo(int val) {
            return new Random().nextInt();
        }

        String bar() {
            return UUID.randomUUID().toString();
        }

        // excluded
        @SuppressWarnings("unused")
        private int baz() {
            return new Random().nextInt();
        }

        // excluded
        void update() {
            lastUpdate.set(System.currentTimeMillis());
        }

    }

    @Singleton
    static class EmbeddingModelImpl implements EmbeddingModel {

        static final AtomicLong lastUpdate = new AtomicLong();

        @Override
        public int foo(int val) {
            return new Random().nextInt();
        }

        @Override
        public String bar() {
            return UUID.randomUUID().toString();
        }

        @Override
        public void update() {
            lastUpdate.set(System.currentTimeMillis());
        }

    }

    public static class CustomKeyGenerator implements CacheKeyGenerator {

        @Override
        public Object generate(Method method, Object... methodParams) {
            return method.getName();
        }

    }

}
