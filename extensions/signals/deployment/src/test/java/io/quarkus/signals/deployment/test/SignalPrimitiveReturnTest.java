package io.quarkus.signals.deployment.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.signals.Receives;
import io.quarkus.signals.Signal;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Verifies that a receiver method accepting and returning a primitive type is matched correctly.
 */
public class SignalPrimitiveReturnTest extends AbstractSignalTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root.addClasses(Receivers.class));

    @Inject
    Signal<Integer> signal;

    @Inject
    Receivers receivers;

    @Test
    public void testPrimitiveSignalAndResponse() {
        int result = signal.reactive().request(42, Integer.class)
                .ifNoItem()
                .after(defaultTimeout())
                .fail()
                .await().indefinitely();
        assertEquals(84, result);
    }

    @Singleton
    public static class Receivers {

        int doubleIt(@Receives int value) {
            return value * 2;
        }
    }
}
