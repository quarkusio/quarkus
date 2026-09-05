package io.quarkus.arc.test.invoker.lookup.dependent.async.invalid;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.inject.spi.DefinitionException;
import jakarta.enterprise.invoke.AsyncHandler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.test.ArcTestContainer;

public class AsyncHandlerArrayTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .asyncHandler(AsyncHandlerArray.class)
            .shouldFail()
            .build();

    @Test
    public void trigger() {
        Throwable error = container.getFailure();
        assertNotNull(error);
        assertInstanceOf(DefinitionException.class, error);
        assertTrue(error.getMessage().contains("AsyncHandlerArray"));
        assertTrue(error.getMessage().contains("Invalid type argument to async handler"));
    }

    public static class AsyncHandlerArray<T> implements AsyncHandler.ReturnType<MyAsyncType<T>[]> {
        public AsyncHandlerArray() {
        }

        @Override
        public MyAsyncType<T>[] transform(MyAsyncType<T>[] original, Runnable completion) {
            return original;
        }
    }
}
