package io.quarkus.qute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

public class CompletedStageTest {

    @Test
    public void testHandleSuccess() {
        CompletedStage<String> stage = CompletedStage.of("foo");
        CompletionStage<String> handled = stage.handle((v, t) -> {
            assertNull(t);
            return v.toUpperCase();
        });
        assertEquals("FOO", handled.toCompletableFuture().join());
    }

    @Test
    public void testHandleFailure() {
        CompletedStage<String> stage = CompletedStage.failure(new RuntimeException("boom"));
        CompletionStage<String> handled = stage.handle((v, t) -> {
            assertNull(v);
            assertEquals("boom", t.getMessage());
            return "recovered";
        });
        assertEquals("recovered", handled.toCompletableFuture().join());
    }

    @Test
    public void testHandleThrows() {
        CompletedStage<String> stage = CompletedStage.of("foo");
        CompletionStage<String> handled = stage.handle((v, t) -> {
            throw new RuntimeException("handle failed");
        });
        assertTrue(handled.toCompletableFuture().isCompletedExceptionally());
    }

    @Test
    public void testRenderAsyncWithoutTimeoutThenHandle() {
        // Reproduces the scenario where renderAsync() without async timeout
        // returns a CompletedStage that external consumers (e.g. RESTEasy Reactive)
        // subscribe to via handle()
        Engine engine = Engine.builder()
                .addDefaultSectionHelpers()
                .addDefaultValueResolvers()
                .useAsyncTimeout(false)
                .build();
        Template template = engine.parse("Hello {name}!");
        CompletionStage<String> cs = template.data("name", "Foo").renderAsync();

        AtomicReference<String> result = new AtomicReference<>();
        AtomicReference<Throwable> error = new AtomicReference<>();
        cs.handle((v, t) -> {
            result.set(v);
            error.set(t);
            return null;
        });
        assertNull(error.get());
        assertEquals("Hello Foo!", result.get());
    }
}
