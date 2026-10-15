package io.quarkus.arc.test.invoker.lookup.dependent.async.invalid;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.inject.spi.DefinitionException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.test.ArcTestContainer;

public class AsyncHandlerNoKindTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .asyncHandler(AsyncHandlerNoKind.class)
            .shouldFail()
            .build();

    @Test
    public void trigger() {
        Throwable error = container.getFailure();
        assertNotNull(error);
        assertInstanceOf(DefinitionException.class, error);
        assertTrue(error.getMessage().contains("AsyncHandlerNoKind"));
        assertTrue(error.getMessage().contains("neither `AsyncHandler.ReturnType` nor `AsyncHandler.ParameterType`"));
    }

    public static class AsyncHandlerNoKind {
        public AsyncHandlerNoKind() {
        }
    }
}
