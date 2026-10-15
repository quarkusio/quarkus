package io.quarkus.arc.test.invoker.lookup.dependent.async.invalid;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.inject.spi.DefinitionException;
import jakarta.enterprise.invoke.AsyncHandler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.test.ArcTestContainer;

public class AsyncHandlerIndirectTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .asyncHandler(AsyncHandlerIndirect.class)
            .shouldFail()
            .build();

    @Test
    public void trigger() {
        Throwable error = container.getFailure();
        assertNotNull(error);
        assertInstanceOf(DefinitionException.class, error);
        assertTrue(error.getMessage().contains("AsyncHandlerIndirect"));
        assertTrue(error.getMessage().contains("neither `AsyncHandler.ReturnType` nor `AsyncHandler.ParameterType`"));
    }

    public static abstract class AsyncHandlerSubclass<T> implements AsyncHandler.ReturnType<T> {
    }

    public static class AsyncHandlerIndirect<T> extends AsyncHandlerSubclass<MyAsyncType<T>> {
        public AsyncHandlerIndirect() {
        }

        @Override
        public MyAsyncType<T> transform(MyAsyncType<T> original, Runnable completion) {
            return original;
        }
    }
}
