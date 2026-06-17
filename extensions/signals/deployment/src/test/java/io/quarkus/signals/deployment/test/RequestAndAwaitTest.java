package io.quarkus.signals.deployment.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import jakarta.enterprise.util.TypeLiteral;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.signals.Receives;
import io.quarkus.signals.Signal;
import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.mutiny.Uni;

/**
 * Verifies that {@link Signal#request(Object, Class)} blocks until the response is available.
 */
public class RequestAndAwaitTest extends AbstractSignalTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root.addClasses(MyReceivers.class, Cmd.class));

    @Inject
    Signal<Cmd> signal;

    @Test
    public void testBlockingReceiver() {
        String result = signal.request(new Cmd("hello"), String.class);
        assertEquals("HELLO", result);
    }

    @Test
    public void testReactiveReceiver() {
        Integer result = signal.request(new Cmd("hello"), Integer.class);
        assertEquals(5, result);
    }

    @Test
    public void testCompletionStageReceiver() {
        Long result = signal.request(new Cmd("hello"), Long.class);
        assertEquals(5L, result);
    }

    @Test
    public void testNoMatchingReceiver() {
        Double result = signal.request(new Cmd("hello"), Double.class);
        assertNull(result);
    }

    @Test
    public void testReceiverFailure() {
        assertThrows(IllegalStateException.class,
                () -> signal.request(new Cmd("fail"), String.class));
    }

    @Test
    public void testRequestWithTypeLiteral() {
        String result = signal.request(new Cmd("hello"), new TypeLiteral<String>() {
        });
        assertEquals("HELLO", result);
    }

    @Test
    public void testRequestWithTypeLiteralNoMatch() {
        Double result = signal.request(new Cmd("hello"), new TypeLiteral<Double>() {
        });
        assertNull(result);
    }

    @Test
    public void testReactiveRequestWithTypeLiteral() {
        String result = signal.reactive().request(new Cmd("hello"), new TypeLiteral<String>() {
        }).ifNoItem().after(defaultTimeout()).fail()
                .await().indefinitely();
        assertEquals("HELLO", result);
    }

    @Singleton
    public static class MyReceivers {

        // Blocking signature → BLOCKING
        String toUpperCase(@Receives Cmd cmd) {
            if ("fail".equals(cmd.value())) {
                throw new IllegalStateException("boom");
            }
            return cmd.value().toUpperCase();
        }

        // Reactive → NON_BLOCKING
        Uni<Integer> toLength(@Receives Cmd cmd) {
            return Uni.createFrom().item(cmd.value().length());
        }

        // CompletionStage → NON_BLOCKING
        CompletionStage<Long> toLengthLong(@Receives Cmd cmd) {
            return CompletableFuture.completedFuture((long) cmd.value().length());
        }
    }

    record Cmd(String value) {
    }
}
