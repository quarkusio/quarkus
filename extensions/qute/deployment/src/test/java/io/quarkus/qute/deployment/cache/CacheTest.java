package io.quarkus.qute.deployment.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheName;
import io.quarkus.qute.Template;
import io.quarkus.qute.cache.QuteCache;
import io.quarkus.test.QuarkusUnitTest;

public class CacheTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(
                    (jar) -> jar.addAsResource(
                            new StringAsset("{#cached}{counter.val}{/cached}::"
                                    + "{#cached key=(myKey or 'alpha')}{counter.getVal(fail)}{/cached}"),
                            "templates/foo.txt"));

    @Inject
    Template foo;

    @CacheName(QuteCache.NAME)
    Cache cache;

    Counter counter = new Counter();

    @Test
    public void testCachedParts() {
        assertEquals("1::2", render(false, null));
        assertEquals("1::2", render(true, "alpha"));
        assertEquals("1::3", render(false, "bravo"));
        cache.invalidateAll().await().indefinitely();
        assertEquals("4::5", render(false, null));
        assertEquals("4::5", render(true, null));
        cache.invalidateAll().await().indefinitely();
        assertThrows(IllegalStateException.class, () -> render(true, null));
        // The failure is cached
        assertThrows(IllegalStateException.class, () -> render(false, null));
        assertEquals("6::7", render(false, "bravo"));
    }

    private String render(boolean fail, String key) {
        return foo.data("counter", counter, "fail", fail, "myKey", key).render();
    }

    public static class Counter {

        private final AtomicInteger val = new AtomicInteger();

        public int getVal() {
            return val.incrementAndGet();
        }

        public int getVal(boolean failure) {
            if (failure) {
                throw new IllegalStateException();
            }
            return val.incrementAndGet();
        }

    }
}
