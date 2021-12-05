package io.quarkus.cache.test.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.cache.CacheResult;
import io.quarkus.test.QuarkusUnitTest;

public class ThrowExecutionExceptionCauseTest {

    private static final String FORCED_EXCEPTION_MESSAGE = "Forced exception";

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClass(CachedService.class));

    @Inject
    CachedService cachedService;

    @Test
    public void testRuntimeExceptionThrowDuringCacheComputation() {
        NumberFormatException e = assertThrows(NumberFormatException.class, () -> {
            cachedService.throwRuntimeExceptionDuringCacheComputation();
        });
        assertEquals(FORCED_EXCEPTION_MESSAGE, e.getMessage());
        // Let's check we didn't put an uncompleted future in the cache because of the previous exception.
        assertThrows(NumberFormatException.class, () -> {
            cachedService.throwRuntimeExceptionDuringCacheComputation();
        });
    }

    @Test
    public void testCheckedExceptionThrowDuringCacheComputation() {
        IOException e = assertThrows(IOException.class, () -> {
            cachedService.throwCheckedExceptionDuringCacheComputation();
        });
        assertEquals(FORCED_EXCEPTION_MESSAGE, e.getMessage());
    }

    @Test
    public void testErrorThrowDuringCacheComputation() {
        OutOfMemoryError e = assertThrows(OutOfMemoryError.class, () -> {
            cachedService.throwErrorDuringCacheComputation();
        });
        assertEquals(FORCED_EXCEPTION_MESSAGE, e.getMessage());
    }

    @ApplicationScoped
    static class CachedService {

        @CacheResult(cacheName = "runtime-exception-cache")
        public String throwRuntimeExceptionDuringCacheComputation() {
            throw new NumberFormatException(FORCED_EXCEPTION_MESSAGE);
        }

        @CacheResult(cacheName = "checked-exception-cache")
        public String throwCheckedExceptionDuringCacheComputation() throws IOException {
            throw new IOException(FORCED_EXCEPTION_MESSAGE);
        }

        @CacheResult(cacheName = "error-cache")
        public String throwErrorDuringCacheComputation() {
            throw new OutOfMemoryError(FORCED_EXCEPTION_MESSAGE);
        }
    }
}
