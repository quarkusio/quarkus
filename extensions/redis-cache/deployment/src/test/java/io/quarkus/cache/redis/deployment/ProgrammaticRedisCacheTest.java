package io.quarkus.cache.redis.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.function.Function;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheName;
import io.quarkus.test.QuarkusUnitTest;

public class ProgrammaticRedisCacheTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset(
                            "quarkus.cache.redis.programmatic-cache.value-type=java.lang.String"),
                            "application.properties"));

    @Inject
    @CacheName("programmatic-cache")
    Cache cache;

    @Test
    public void testCompute() {

        String KEY_COMPUTE = "KEY_COMPUTE";
        String VALUE_COMPUTE = "VALUE_COMPUTE";

        String value = cache.compute(KEY_COMPUTE, (key, previousValue) -> {
            assertEquals(KEY_COMPUTE, key);
            assertNull(previousValue);
            return VALUE_COMPUTE;
        });
        assertEquals(VALUE_COMPUTE, value);
    }

    @Test
    public void testConcurrentCompute() {
        String KEY = "KEY_CONCURRENT_COMPUTE";
        String VALUE_COMPUTE = "VALUE_COMPUTE";
        String CONCURRENT_VALUE = "CONCURRENT_VALUE";

        RuntimeException exception =   assertThrows(RuntimeException.class, () -> cache.compute(KEY, (key, previousValue) -> {
            CountDownLatch latch = new CountDownLatch(1);
            new Thread(() -> {
                cache.compute(KEY, (k, p) -> CONCURRENT_VALUE);
                latch.countDown();
            }).start();
            try {
                latch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return VALUE_COMPUTE;
        }));
        assertEquals("Concurrent modification, cache has not been modified", exception.getMessage());
        assertEquals(CONCURRENT_VALUE, cache.get(KEY, Function.identity()).await().indefinitely());
        cache.invalidate(KEY);
    }

    @Test
    public void testFailingCompute() {
        String KEY = "KEY_FAILING_COMPUTE";

        RuntimeException exception = assertThrows(RuntimeException.class, () -> cache.compute(KEY, (key, previousValue) -> {
            throw new ArithmeticException();
        }));
        assertEquals("Execution failed, cache has not been modified", exception.getMessage());
        assertInstanceOf(ArithmeticException.class, exception.getCause());
        cache.invalidate(KEY);
    }

}
