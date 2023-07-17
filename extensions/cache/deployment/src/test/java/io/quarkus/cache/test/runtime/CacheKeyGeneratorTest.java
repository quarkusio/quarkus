package io.quarkus.cache.test.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.reflect.Method;
import java.math.BigInteger;
import java.security.SecureRandom;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.cache.CacheInvalidate;
import io.quarkus.cache.CacheKey;
import io.quarkus.cache.CacheKeyGenerator;
import io.quarkus.cache.CacheResult;
import io.quarkus.cache.CompositeCacheKey;
import io.quarkus.test.QuarkusUnitTest;

public class CacheKeyGeneratorTest {

    private static final String ASPARAGUS = "asparagus";
    private static final String CAULIFLOWER = "cauliflower";
    private static final Object OBJECT = new Object();

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest().withApplicationRoot(jar -> jar
            .addAsResource(new StringAsset("cache-key-element=" + ASPARAGUS), "application.properties")
            .addClass(CachedService.class));

    @Inject
    CachedService cachedService;

    @Test
    @ActivateRequestContext
    public void testAllCacheKeyGeneratorKinds() {
        String value1 = cachedService.cachedMethod1(OBJECT, /* Not used */ null);
        String value2 = cachedService.cachedMethod1(OBJECT, /* Not used */ null);
        assertSame(value1, value2);

        BigInteger value3 = cachedService.cachedMethod2();
        BigInteger value4 = cachedService.cachedMethod2();
        assertSame(value3, value4);

        cachedService.invalidate1(CAULIFLOWER, OBJECT);

        String value5 = cachedService.cachedMethod1(OBJECT, /* Not used */ null);
        assertNotSame(value2, value5);

        BigInteger value6 = cachedService.cachedMethod2();
        assertNotSame(value4, value6);

        // If this fails, the interceptor may be leaking @Dependent beans by not destroying them when it should.
        assertEquals(0, DependentKeyGen.livingBeans);

        Object value7 = cachedService.cachedMethod3(/* Not used */ null, /* Not used */ null);
        Object value8 = cachedService.cachedMethod3(/* Not used */ null, /* Not used */ null);
        assertSame(value7, value8);

        cachedService.invalidate2(CAULIFLOWER, /* Not used */ null, "cachedMethod3");

        Object value9 = cachedService.cachedMethod3(/* Not used */ null, /* Not used */ null);
        assertNotSame(value8, value9);
    }

    @ApplicationScoped
    static class CachedService {

        private static final String CACHE_NAME = "test-cache";

        // This method is used to test a CDI injection into a cache key generator.
        public String getCauliflower() {
            return CAULIFLOWER;
        }

        @CacheResult(cacheName = CACHE_NAME, keyGenerator = SingletonKeyGen.class)
        public String cachedMethod1(/* Key element */ Object param0, /* Not used */ Integer param1) {
            return new String();
        }

        @CacheResult(cacheName = CACHE_NAME, keyGenerator = DependentKeyGen.class)
        public BigInteger cachedMethod2() {
            return BigInteger.valueOf(new SecureRandom().nextInt());
        }

        // The cache key elements will vary depending on which annotation is evaluated during the interception.
        @CacheInvalidate(cacheName = CACHE_NAME, keyGenerator = RequestScopedKeyGen.class)
        @CacheInvalidate(cacheName = CACHE_NAME)
        public void invalidate1(@CacheKey String param0, Object param1) {
        }

        @CacheResult(cacheName = CACHE_NAME, keyGenerator = ApplicationScopedKeyGen.class)
        public Object cachedMethod3(/* Not used */ Object param0, /* Not used */ String param1) {
            return new Object();
        }

        @CacheInvalidate(cacheName = CACHE_NAME, keyGenerator = NotABeanKeyGen.class)
        public void invalidate2(/* Key element */ String param0, /* Not used */ Long param1, /* Key element */ String param2) {
        }
    }

    @Singleton
    public static class SingletonKeyGen implements CacheKeyGenerator {

        @ConfigProperty(name = "cache-key-element")
        String cacheKeyElement;

        @Override
        public Object generate(Method method, Object... methodParams) {
            return new CompositeCacheKey(cacheKeyElement, methodParams[0]);
        }
    }

    @ApplicationScoped
    public static class ApplicationScopedKeyGen implements CacheKeyGenerator {

        @Inject
        CachedService cachedService;

        @Override
        public Object generate(Method method, Object... methodParams) {
            return new CompositeCacheKey(method.getName(), cachedService.getCauliflower());
        }
    }

    @RequestScoped
    public static class RequestScopedKeyGen implements CacheKeyGenerator {

        @Override
        public Object generate(Method method, Object... methodParams) {
            return new CompositeCacheKey(ASPARAGUS, methodParams[1]);
        }
    }

    @Dependent
    public static class DependentKeyGen implements CacheKeyGenerator {

        // This counts how many beans of this key generator are currently alive.
        public static volatile int livingBeans;

        @PostConstruct
        void postConstruct() {
            livingBeans++;
        }

        @PreDestroy
        void preDestroy() {
            livingBeans--;
        }

        @Override
        public Object generate(Method method, Object... methodParams) {
            return CAULIFLOWER;
        }
    }

    public static class NotABeanKeyGen implements CacheKeyGenerator {

        @Override
        public Object generate(Method method, Object... methodParams) {
            return new CompositeCacheKey(methodParams[2], methodParams[0]);
        }
    }
}
