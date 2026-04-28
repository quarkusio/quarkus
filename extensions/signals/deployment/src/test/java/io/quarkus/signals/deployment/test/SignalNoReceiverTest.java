package io.quarkus.signals.deployment.test;

import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Duration;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.signals.Signal;
import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.mutiny.Uni;

/**
 * Verifies that emitting a signal with no matching receivers does not fail.
 */
public class SignalNoReceiverTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root.addClasses(Orphan.class));

    @Inject
    Signal<Orphan> orphan;

    @Test
    public void testPublishNoReceiver() {
        // Should not throw
        orphan.publish(new Orphan("nobody"));
    }

    @Test
    public void testSendNoReceiver() {
        // Should not throw
        orphan.send(new Orphan("nobody"));
    }

    @Test
    public void testPublishUniNoReceiver() {
        Uni<Void> result = orphan.reactive().publish(new Orphan("nobody"));
        assertNull(result.ifNoItem().after(Duration.ofSeconds(1)).fail()
                .await().indefinitely());
    }

    @Test
    public void testSendUniNoReceiver() {
        Uni<Void> result = orphan.reactive().send(new Orphan("nobody"));
        assertNull(result.ifNoItem().after(Duration.ofSeconds(1)).fail()
                .await().indefinitely());
    }

    @Test
    public void testRequestNoReceiver() {
        assertNull(orphan.request(new Orphan("nobody"), String.class));
    }

    @Test
    public void testRequestUniNoReceiver() {
        String result = orphan.reactive().request(new Orphan("nobody"), String.class)
                .ifNoItem()
                .after(Duration.ofSeconds(1))
                .fail()
                .await().indefinitely();
        assertNull(result);
    }

    record Orphan(String value) {
    }
}
