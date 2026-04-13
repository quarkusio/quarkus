package io.quarkus.arc.test.invoker.lookup.dependent.async.invalid;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.inject.spi.DefinitionException;
import jakarta.enterprise.invoke.AsyncHandler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.test.ArcTestContainer;

public class AsyncHandlerTypeVariableTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .asyncHandler(AsyncHandlerTypeVariable.class)
            .shouldFail()
            .build();

    @Test
    public void trigger() {
        Throwable error = container.getFailure();
        assertNotNull(error);
        assertInstanceOf(DefinitionException.class, error);
        assertTrue(error.getMessage().contains("AsyncHandlerTypeVariable"));
        assertTrue(error.getMessage().contains("Invalid type argument to async handler"));
    }

    public static class AsyncHandlerTypeVariable<T> implements AsyncHandler.ReturnType<T> {
        public AsyncHandlerTypeVariable() {
        }

        @Override
        public T transform(T original, Runnable completion) {
            return original;
        }
    }
}
