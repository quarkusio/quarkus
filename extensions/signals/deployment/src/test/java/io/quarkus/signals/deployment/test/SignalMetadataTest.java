package io.quarkus.signals.deployment.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.signals.Receives;
import io.quarkus.signals.Signal;
import io.quarkus.signals.SignalContext;
import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.mutiny.Uni;

/**
 * Verifies that metadata attached via {@link Signal.Emission#withMeta(String, Object)}
 * is accessible in the receiver through {@link SignalContext#metadata()}.
 */
public class SignalMetadataTest extends AbstractSignalTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root.addClasses(Receivers.class, Event.class));

    @Inject
    Signal<Event> event;

    @Inject
    Receivers receivers;

    @Test
    public void testMetadata() {
        receivers.captured.clear();

        Signal<Event> eventWithMeta = event.setMetadata(Map.of("traceId", "abc-123", "source", "test"));

        Uni<String> uni = eventWithMeta
                .reactive().request(new Event("hello"), String.class);

        String result = uni.ifNoItem()
                .after(defaultTimeout())
                .fail()
                .await().indefinitely();

        assertEquals("hello:abc-123", result);
        assertEquals(1, receivers.captured.size());
        assertEquals("abc-123", receivers.captured.get(0));

        receivers.captured.clear();

        result = eventWithMeta.putMetadata("traceId", "def-123")
                .reactive().request(new Event("hi"), String.class)
                .ifNoItem()
                .after(defaultTimeout())
                .fail()
                .await().indefinitely();
        assertEquals("hi:def-123", result);
        assertEquals(1, receivers.captured.size());
        assertEquals("def-123", receivers.captured.get(0));
    }

    @Test
    public void testSetMetadataReplacesAll() {
        receivers.captured.clear();

        // First: setMetadata with traceId + source
        Signal<Event> first = event.setMetadata(Map.of("traceId", "aaa", "source", "test"));
        // Second: setMetadata replaces all metadata — only "traceId" and "source" present
        Signal<Event> second = first.setMetadata(Map.of("traceId", "bbb", "source", "test"));

        String result = second.reactive().request(new Event("replaced"), String.class)
                .ifNoItem().after(defaultTimeout()).fail()
                .await().indefinitely();
        assertEquals("replaced:bbb", result);

        // Original signal's metadata is not affected
        result = first.reactive().request(new Event("original"), String.class)
                .ifNoItem().after(defaultTimeout()).fail()
                .await().indefinitely();
        assertEquals("original:aaa", result);
    }

    @Test
    public void testPutMetadataReplacesExistingKey() {
        receivers.captured.clear();

        Signal<Event> withTrace = event.setMetadata(Map.of("traceId", "first", "source", "test"));
        // putMetadata with same key should replace the value
        Signal<Event> replaced = withTrace.putMetadata("traceId", "second");

        String result = replaced.reactive().request(new Event("check"), String.class)
                .ifNoItem().after(defaultTimeout()).fail()
                .await().indefinitely();
        assertEquals("check:second", result);

        // The original child signal still has the old value
        result = withTrace.reactive().request(new Event("old"), String.class)
                .ifNoItem().after(defaultTimeout()).fail()
                .await().indefinitely();
        assertEquals("old:first", result);
    }

    @Singleton
    public static class Receivers {

        final List<String> captured = new CopyOnWriteArrayList<>();

        Uni<String> withMeta(@Receives SignalContext<Event> ctx) {
            if (!ctx.metadata().get("source").equals("test")) {
                throw new IllegalStateException();
            }
            String traceId = (String) ctx.metadata().get("traceId");
            captured.add(traceId);
            return Uni.createFrom().item(ctx.signal().name() + ":" + traceId);
        }
    }

    record Event(String name) {
    }
}
