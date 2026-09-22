package io.quarkus.cache.test.runtime;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.cache.CacheException;
import io.quarkus.cache.CacheKey;
import io.quarkus.cache.CacheKeyGenerator;
import io.quarkus.cache.CacheResult;
import io.quarkus.cache.CompositeCacheKey;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Tests that a CDI client proxy is rejected as a cache key element, whatever the scope of the bean, including when it
 * reaches the cache inside a composite key or from a key generator.
 */
public class ClientProxyCacheKeyTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(CachedService.class, RequestScopedHolder.class,
                    ApplicationScopedHolder.class, HolderKeyGenerator.class, ProxyKeyGenerator.class,
                    ProxyInCompositeKeyGenerator.class));

    @Inject
    CachedService cachedService;

    @Inject
    RequestScopedHolder requestScopedHolder;

    @Inject
    ApplicationScopedHolder applicationScopedHolder;

    @Test
    @ActivateRequestContext
    public void testRequestScopedProxyAsSimpleKeyIsRejected() {
        CacheException e = assertThrows(CacheException.class, () -> cachedService.simpleKey(requestScopedHolder));
        assertRejected(e, "simpleKey", RequestScopedHolder.class);
    }

    @Test
    @ActivateRequestContext
    public void testRequestScopedProxyAsExplicitCompositeKeyElementIsRejected() {
        CacheException e = assertThrows(CacheException.class,
                () -> cachedService.explicitCompositeKey("foo", requestScopedHolder, "ignored"));
        assertRejected(e, "explicitCompositeKey", RequestScopedHolder.class);
    }

    @Test
    @ActivateRequestContext
    public void testRequestScopedProxyAsImplicitCompositeKeyElementIsRejected() {
        CacheException e = assertThrows(CacheException.class,
                () -> cachedService.implicitCompositeKey("foo", requestScopedHolder));
        assertRejected(e, "implicitCompositeKey", RequestScopedHolder.class);
    }

    @Test
    public void testApplicationScopedProxyAsKeyIsRejected() {
        CacheException e = assertThrows(CacheException.class, () -> cachedService.simpleKey(applicationScopedHolder));
        assertRejected(e, "simpleKey", ApplicationScopedHolder.class);
    }

    @Test
    @ActivateRequestContext
    public void testProxyReturnedByAKeyGeneratorIsRejected() {
        CacheException e = assertThrows(CacheException.class, () -> cachedService.generatedKey(requestScopedHolder));
        assertRejected(e, "generatedKey", RequestScopedHolder.class);
    }

    @Test
    @ActivateRequestContext
    public void testProxyInsideACompositeKeyFromAGeneratorIsRejected() {
        CacheException e = assertThrows(CacheException.class,
                () -> cachedService.generatedCompositeKey(requestScopedHolder));
        assertRejected(e, "generatedCompositeKey", RequestScopedHolder.class);
    }

    @Test
    @ActivateRequestContext
    public void testDerivedValueIsAccepted() {
        String value1 = cachedService.generatedValueKey(requestScopedHolder);
        String value2 = cachedService.generatedValueKey(requestScopedHolder);
        assertSame(value1, value2);
    }

    private static void assertRejected(CacheException e, String methodName, Class<?> beanClass) {
        IllegalArgumentException cause = assertInstanceOf(IllegalArgumentException.class, e.getCause());
        assertTrue(cause.getMessage().contains(methodName), cause.getMessage());
        assertTrue(cause.getMessage().contains(beanClass.getName()), cause.getMessage());
        assertTrue(cause.getMessage().contains("toString()"), cause.getMessage());
    }

    @ApplicationScoped
    static class CachedService {

        @CacheResult(cacheName = "simple")
        public String simpleKey(Object key) {
            return new String("value");
        }

        @CacheResult(cacheName = "explicit-composite")
        public String explicitCompositeKey(@CacheKey String first, @CacheKey Object second, String notPartOfTheKey) {
            return new String("value");
        }

        @CacheResult(cacheName = "implicit-composite")
        public String implicitCompositeKey(String first, Object second) {
            return new String("value");
        }

        @CacheResult(cacheName = "generated", keyGenerator = ProxyKeyGenerator.class)
        public String generatedKey(RequestScopedHolder holder) {
            return new String("value");
        }

        @CacheResult(cacheName = "generated-composite", keyGenerator = ProxyInCompositeKeyGenerator.class)
        public String generatedCompositeKey(RequestScopedHolder holder) {
            return new String("value");
        }

        @CacheResult(cacheName = "generated-value", keyGenerator = HolderKeyGenerator.class)
        public String generatedValueKey(RequestScopedHolder holder) {
            return new String("value");
        }
    }

    @RequestScoped
    static class RequestScopedHolder {

        public String getValue() {
            return "request";
        }
    }

    @ApplicationScoped
    static class ApplicationScopedHolder {
    }

    public static class HolderKeyGenerator implements CacheKeyGenerator {

        public HolderKeyGenerator() {
        }

        @Override
        public Object generate(Method method, Object... methodParams) {
            return ((RequestScopedHolder) methodParams[0]).getValue();
        }
    }

    public static class ProxyKeyGenerator implements CacheKeyGenerator {

        public ProxyKeyGenerator() {
        }

        @Override
        public Object generate(Method method, Object... methodParams) {
            return methodParams[0];
        }
    }

    public static class ProxyInCompositeKeyGenerator implements CacheKeyGenerator {

        public ProxyInCompositeKeyGenerator() {
        }

        @Override
        public Object generate(Method method, Object... methodParams) {
            return new CompositeCacheKey("prefix", methodParams[0]);
        }
    }
}
