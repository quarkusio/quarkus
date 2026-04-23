package io.quarkus.signals.deployment.test;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Inject;
import jakarta.inject.Qualifier;
import jakarta.inject.Singleton;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.signals.Receives;
import io.quarkus.signals.Signal;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Verifies that a receiver declaring the {@code @Any} qualifier is notified
 * of all signals with a matching type, regardless of the signal's qualifiers.
 */
public class SignalAnyReceiverTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root.addClasses(Receivers.class, Item.class, Important.class));

    @Inject
    Signal<Item> item;

    @Inject
    Receivers receivers;

    @Test
    public void testAnyReceiverMatchesAllSignals() {
        receivers.sequence.clear();

        // Emit an unqualified signal
        item.publish(new Item("plain"));
        Awaitility.await().until(() -> receivers.sequence.size() >= 1);
        assertEquals(1, receivers.sequence.size());
        assertEquals("any_plain", receivers.sequence.get(0));

        receivers.sequence.clear();

        // Emit a qualified signal — the @Any receiver should still be notified
        item.select(Important.Literal.INSTANCE).publish(new Item("vip"));
        Awaitility.await().until(() -> receivers.sequence.size() >= 1);
        assertEquals(1, receivers.sequence.size());
        assertEquals("any_vip", receivers.sequence.get(0));
    }

    @Singleton
    public static class Receivers {

        final List<String> sequence = new CopyOnWriteArrayList<>();

        void catchAll(@Receives @Any Item item) {
            sequence.add("any_" + item.name());
        }
    }

    record Item(String name) {
    }

    @Qualifier
    @Target({ FIELD, METHOD, PARAMETER })
    @Retention(RUNTIME)
    public @interface Important {

        final class Literal extends AnnotationLiteral<Important> implements Important {
            public static final Literal INSTANCE = new Literal();
            private static final long serialVersionUID = 1L;
        }
    }
}
