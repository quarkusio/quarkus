package io.quarkus.signals.deployment.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
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
public class SignalMetadataTest {

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
                .requestUni(new Event("hello"), String.class);

        String result = uni.ifNoItem()
                .after(Duration.ofSeconds(1))
                .fail()
                .await().indefinitely();

        assertEquals("hello:abc-123", result);
        assertEquals(1, receivers.captured.size());
        assertEquals("abc-123", receivers.captured.get(0));

        receivers.captured.clear();

        result = eventWithMeta.putMetadata("traceId", "def-123")
                .requestUni(new Event("hi"), String.class)
                .ifNoItem()
                .after(Duration.ofSeconds(1))
                .fail()
                .await().indefinitely();
        assertEquals("hi:def-123", result);
        assertEquals(1, receivers.captured.size());
        assertEquals("def-123", receivers.captured.get(0));
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
