package io.quarkus.signals.deployment.test.metrics;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.time.Duration;
import java.util.List;

import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Inject;
import jakarta.inject.Qualifier;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.signals.Receives;
import io.quarkus.signals.Signal;
import io.quarkus.signals.SignalContext.EmissionType;
import io.quarkus.signals.runtime.metrics.MetricsSupport;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Verifies that Micrometer counters are registered for signal emissions, receiver invocations and failures.
 */
public class SignalsMetricsTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root
                    .addClasses(PingReceivers.class, Ping.class, Pong.class, Urgent.class))
            .setForcedDependencies(
                    List.of(Dependency.of("io.quarkus", "quarkus-micrometer-deployment", Version.getVersion())));

    @Inject
    MeterRegistry registry;

    @Inject
    Signal<Ping> ping;

    @Inject
    Signal<Pong> pong;

    @BeforeAll
    static void addSimpleRegistry() {
        Metrics.globalRegistry.add(new SimpleMeterRegistry());
    }

    @BeforeEach
    void clear() {
        // Reset the counters so each test asserts absolute values
        registry.clear();
    }

    @Test
    public void testRequestCounted() {
        // Setup: request(Ping, String.class); only the "ping" receiver returns a String, so a single receiver runs.
        // Expected: one emission (REQUEST, response.type=String) and one receiver execution, no errors.
        String result = ping.reactive().request(new Ping("hello"), String.class)
                .ifNoItem().after(Duration.ofSeconds(5)).fail()
                .await().indefinitely();
        assertEquals("hello", result);

        assertEquals(1, count(MetricsSupport.EMISSIONS, MetricsSupport.TAG_SIGNAL_TYPE, Ping.class.getTypeName(),
                MetricsSupport.TAG_EMISSION_TYPE, EmissionType.REQUEST.toString(),
                MetricsSupport.TAG_RESPONSE_TYPE, String.class.getTypeName()));
        assertEquals(1, count(MetricsSupport.RECEIVER_EXECUTIONS, MetricsSupport.TAG_SIGNAL_TYPE,
                Ping.class.getTypeName(), MetricsSupport.TAG_EMISSION_TYPE, EmissionType.REQUEST.toString(),
                MetricsSupport.TAG_ERROR_TYPE, MetricsSupport.ERROR_TYPE_NONE));
    }

    @Test
    public void testPublishCountsExecutionPerReceiver() {
        // Setup: publish (multicast) a Ping; both unqualified receivers ("ping" and "observe") match.
        // Expected: a single emission (PUBLISH, response.type=none) but two receiver executions.
        ping.reactive().publish(new Ping("multi"))
                .ifNoItem().after(Duration.ofSeconds(5)).fail()
                .await().indefinitely();

        assertEquals(1, count(MetricsSupport.EMISSIONS, MetricsSupport.TAG_SIGNAL_TYPE, Ping.class.getTypeName(),
                MetricsSupport.TAG_EMISSION_TYPE, EmissionType.PUBLISH.toString(),
                MetricsSupport.TAG_RESPONSE_TYPE, MetricsSupport.RESPONSE_TYPE_NONE));
        assertEquals(2, count(MetricsSupport.RECEIVER_EXECUTIONS, MetricsSupport.TAG_SIGNAL_TYPE,
                Ping.class.getTypeName(), MetricsSupport.TAG_EMISSION_TYPE, EmissionType.PUBLISH.toString(),
                MetricsSupport.TAG_ERROR_TYPE, MetricsSupport.ERROR_TYPE_NONE));
    }

    @Test
    public void testSendCounted() {
        // Setup: send (fire-and-forget) a Pong; a single Pong receiver matches.
        // Expected: one emission (SEND, response.type=none) and one receiver execution.
        pong.reactive().send(new Pong("fire"))
                .ifNoItem().after(Duration.ofSeconds(5)).fail()
                .await().indefinitely();

        assertEquals(1, count(MetricsSupport.EMISSIONS, MetricsSupport.TAG_SIGNAL_TYPE, Pong.class.getTypeName(),
                MetricsSupport.TAG_EMISSION_TYPE, EmissionType.SEND.toString(),
                MetricsSupport.TAG_RESPONSE_TYPE, MetricsSupport.RESPONSE_TYPE_NONE));
        assertEquals(1, count(MetricsSupport.RECEIVER_EXECUTIONS, MetricsSupport.TAG_SIGNAL_TYPE,
                Pong.class.getTypeName(), MetricsSupport.TAG_EMISSION_TYPE, EmissionType.SEND.toString(),
                MetricsSupport.TAG_ERROR_TYPE, MetricsSupport.ERROR_TYPE_NONE));
    }

    @Test
    public void testErrorCounted() {
        // Setup: the "ping" receiver throws IllegalStateException for the "boom" payload.
        // Expected: the execution is counted with error.type set to the exception class, and not with error.type=none.
        assertThrows(RuntimeException.class, () -> ping.reactive().request(new Ping("boom"), String.class)
                .ifNoItem().after(Duration.ofSeconds(5)).fail()
                .await().indefinitely());

        assertEquals(1, count(MetricsSupport.RECEIVER_EXECUTIONS, MetricsSupport.TAG_SIGNAL_TYPE,
                Ping.class.getTypeName(), MetricsSupport.TAG_EMISSION_TYPE, EmissionType.REQUEST.toString(),
                MetricsSupport.TAG_ERROR_TYPE, IllegalStateException.class.getName()));
        assertEquals(0, count(MetricsSupport.RECEIVER_EXECUTIONS, MetricsSupport.TAG_SIGNAL_TYPE,
                Ping.class.getTypeName(), MetricsSupport.TAG_EMISSION_TYPE, EmissionType.REQUEST.toString(),
                MetricsSupport.TAG_ERROR_TYPE, MetricsSupport.ERROR_TYPE_NONE));
    }

    private double count(String name, String... tags) {
        Counter counter = registry.find(name).tags(tags).counter();
        return counter == null ? 0 : counter.count();
    }

    record Ping(String value) {
    }

    record Pong(String value) {
    }

    @Qualifier
    @Target({ FIELD, METHOD, PARAMETER })
    @Retention(RUNTIME)
    public @interface Urgent {

        final class Literal extends AnnotationLiteral<Urgent> implements Urgent {
            public static final Literal INSTANCE = new Literal();
            private static final long serialVersionUID = 1L;
        }
    }

    @Singleton
    public static class PingReceivers {

        // The unqualified request receiver: returns a String (so request(..., String.class) resolves to it) and throws
        // for the "boom" payload to exercise the failure path
        String ping(@Receives Ping ping) {
            if ("boom".equals(ping.value())) {
                throw new IllegalStateException("boom");
            }
            return ping.value();
        }

        // A second unqualified receiver so that a publish (multicast) delivers to two receivers
        void observe(@Receives Ping ping) {
        }

        // A @Urgent-qualified receiver, only reached when the emission is narrowed with select(@Urgent)
        String urgentPing(@Receives @Urgent Ping ping) {
            return ping.value();
        }

        // A Pong receiver to exercise the fire-and-forget (send) path
        void pong(@Receives Pong pong) {
        }
    }
}
