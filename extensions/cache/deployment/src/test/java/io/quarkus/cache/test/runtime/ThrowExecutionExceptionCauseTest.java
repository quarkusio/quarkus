package io.quarkus.cache.test.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.cache.CacheResult;
import io.quarkus.test.QuarkusUnitTest;

public class ThrowExecutionExceptionCauseTest {

    private static final String FORCED_EXCEPTION_MESSAGE = "Forced exception";

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClass(CachedService.class));

    @Inject
    CachedService cachedService;

    @Test
    public void testRuntimeExceptionThrow() {
        NumberFormatException e = assertThrows(NumberFormatException.class, () -> {
            cachedService.runtimeExceptionCachedMethod();
        });
        assertEquals(FORCED_EXCEPTION_MESSAGE, e.getMessage());
    }

    @Test
    public void testCheckedExceptionThrow() {
        IOException e = assertThrows(IOException.class, () -> {
            cachedService.checkedExceptionCachedMethod();
        });
        assertEquals(FORCED_EXCEPTION_MESSAGE, e.getMessage());
    }

    @ApplicationScoped
    static class CachedService {

        private static final String CACHE_NAME = "test-cache";

        @CacheResult(cacheName = CACHE_NAME)
        public String runtimeExceptionCachedMethod() {
            throw new NumberFormatException(FORCED_EXCEPTION_MESSAGE);
        }

        @CacheResult(cacheName = CACHE_NAME)
        public String checkedExceptionCachedMethod() throws Exception {
            throw new IOException(FORCED_EXCEPTION_MESSAGE);
        }
    }
}
