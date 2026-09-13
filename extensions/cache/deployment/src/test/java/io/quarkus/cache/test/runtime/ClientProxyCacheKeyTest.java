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
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Tests that a client proxy of a bean whose scope is not application-wide is rejected as a cache key element.
 */
public class ClientProxyCacheKeyTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(CachedService.class, RequestScopedHolder.class,
                    ApplicationScopedHolder.class, HolderKeyGenerator.class));

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
        assertRejected(e, "simpleKey");
    }

    @Test
    @ActivateRequestContext
    public void testRequestScopedProxyAsExplicitCompositeKeyElementIsRejected() {
        CacheException e = assertThrows(CacheException.class,
                () -> cachedService.explicitCompositeKey("foo", requestScopedHolder, "ignored"));
        assertRejected(e, "explicitCompositeKey");
    }

    @Test
    @ActivateRequestContext
    public void testRequestScopedProxyAsImplicitCompositeKeyElementIsRejected() {
        CacheException e = assertThrows(CacheException.class,
                () -> cachedService.implicitCompositeKey("foo", requestScopedHolder));
        assertRejected(e, "implicitCompositeKey");
    }

    @Test
    public void testApplicationScopedProxyAsKeyIsAccepted() {
        String value1 = cachedService.simpleKey(applicationScopedHolder);
        String value2 = cachedService.simpleKey(applicationScopedHolder);
        assertSame(value1, value2);
    }

    @Test
    @ActivateRequestContext
    public void testKeyGeneratorIsNotChecked() {
        String value1 = cachedService.generatedKey(requestScopedHolder);
        String value2 = cachedService.generatedKey(requestScopedHolder);
        assertSame(value1, value2);
    }

    private static void assertRejected(CacheException e, String methodName) {
        IllegalArgumentException cause = assertInstanceOf(IllegalArgumentException.class, e.getCause());
        assertTrue(cause.getMessage().contains(methodName), cause.getMessage());
        assertTrue(cause.getMessage().contains("RequestScoped"), cause.getMessage());
        assertTrue(cause.getMessage().contains(RequestScopedHolder.class.getName()), cause.getMessage());
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

        @CacheResult(cacheName = "generated", keyGenerator = HolderKeyGenerator.class)
        public String generatedKey(RequestScopedHolder holder) {
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
}
