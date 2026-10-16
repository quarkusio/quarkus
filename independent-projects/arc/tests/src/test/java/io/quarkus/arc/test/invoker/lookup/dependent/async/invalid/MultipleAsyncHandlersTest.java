package io.quarkus.arc.test.invoker.lookup.dependent.async.invalid;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.enterprise.invoke.AsyncHandler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.test.ArcTestContainer;

public class MultipleAsyncHandlersTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .asyncHandler(AsyncHandler1.class)
            .asyncHandler(AsyncHandler2.class)
            .shouldFail()
            .build();

    @Test
    public void trigger() {
        Throwable error = container.getFailure();
        assertNotNull(error);
        assertInstanceOf(DeploymentException.class, error);
        assertTrue(error.getMessage().contains("Multiple async handlers defined for async type"));
        assertTrue(error.getMessage().contains("AsyncHandler1"));
        assertTrue(error.getMessage().contains("AsyncHandler2"));
        assertTrue(error.getMessage().contains("You have to configure the async handler class for this async type explicitly"));
    }

    public static class AsyncHandler1<T> implements AsyncHandler.ReturnType<MyAsyncType<T>> {
        public AsyncHandler1() {
        }

        @Override
        public MyAsyncType<T> transform(MyAsyncType<T> original, Runnable completion) {
            return original;
        }
    }

    public static class AsyncHandler2<T> implements AsyncHandler.ReturnType<MyAsyncType<T>> {
        public AsyncHandler2() {
        }

        @Override
        public MyAsyncType<T> transform(MyAsyncType<T> original, Runnable completion) {
            return original;
        }
    }
}
