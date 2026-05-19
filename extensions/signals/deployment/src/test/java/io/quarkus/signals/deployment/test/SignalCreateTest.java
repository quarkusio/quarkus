package io.quarkus.signals.deployment.test;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.enterprise.util.TypeLiteral;
import jakarta.inject.Inject;
import jakarta.inject.Qualifier;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.signals.Receivers;
import io.quarkus.signals.Signal;
import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.mutiny.Uni;

/**
 * Verifies that {@link Signal#create(Class, java.lang.annotation.Annotation...)} and
 * {@link Signal#create(TypeLiteral, java.lang.annotation.Annotation...)} can be used
 * to obtain a {@link Signal} instance without CDI injection.
 */
public class SignalCreateTest extends AbstractSignalTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root.addClasses(Ping.class, Urgent.class));

    @Inject
    Receivers receivers;

    @Test
    public void testCreateWithClass() {
        List<String> received = new CopyOnWriteArrayList<>();

        var reg = receivers.newReceiver(Ping.class)
                .notify(ctx -> {
                    received.add(ctx.signal().id());
                });
        try {
            Signal<Ping> signal = Signal.create(Ping.class);
            signal.publish(new Ping("class"));
            Awaitility.await().until(() -> received.size() >= 1);
            assertEquals(1, received.size());
            assertEquals("class", received.get(0));
        } finally {
            reg.unregister();
        }
    }

    @Test
    public void testCreateWithClassRequest() {
        var reg = receivers.newReceiver(Ping.class)
                .setResponseType(String.class)
                .notify(ctx -> {
                    return Uni.createFrom().item("reply_" + ctx.signal().id());
                });
        try {
            Signal<Ping> signal = Signal.create(Ping.class);
            String result = signal.reactive().request(new Ping("class-req"), String.class)
                    .ifNoItem().after(defaultTimeout()).fail()
                    .await().indefinitely();
            assertEquals("reply_class-req", result);
        } finally {
            reg.unregister();
        }
    }

    @Test
    public void testCreateWithClassAndQualifiers() {
        List<String> received = new CopyOnWriteArrayList<>();

        var regDefault = receivers.newReceiver(Ping.class)
                .notify(ctx -> {
                    received.add("default_" + ctx.signal().id());
                });
        var regUrgent = receivers.newReceiver(Ping.class)
                .setQualifiers(Urgent.Literal.INSTANCE)
                .notify(ctx -> {
                    received.add("urgent_" + ctx.signal().id());
                });
        try {
            Signal<Ping> signal = Signal.create(Ping.class, Urgent.Literal.INSTANCE);
            signal.publish(new Ping("q"));
            Awaitility.await().until(() -> received.size() >= 1);
            assertEquals(1, received.size());
            assertTrue(received.contains("urgent_q"),
                    "Only the @Urgent receiver should be invoked");
        } finally {
            regDefault.unregister();
            regUrgent.unregister();
        }
    }

    @Test
    public void testCreateWithTypeLiteral() {
        List<String> received = new CopyOnWriteArrayList<>();

        var reg = receivers.newReceiver(Ping.class)
                .notify(ctx -> {
                    received.add(ctx.signal().id());
                });
        try {
            Signal<Ping> signal = Signal.create(new TypeLiteral<Ping>() {
            });
            signal.publish(new Ping("tl"));
            Awaitility.await().until(() -> received.size() >= 1);
            assertEquals(1, received.size());
            assertEquals("tl", received.get(0));
        } finally {
            reg.unregister();
        }
    }

    @Test
    public void testCreateWithTypeLiteralRequest() {
        var reg = receivers.newReceiver(Ping.class)
                .setResponseType(String.class)
                .notify(ctx -> {
                    return Uni.createFrom().item("reply_" + ctx.signal().id());
                });
        try {
            Signal<Ping> signal = Signal.create(new TypeLiteral<Ping>() {
            });
            String result = signal.reactive().request(new Ping("tl-req"), String.class)
                    .ifNoItem().after(defaultTimeout()).fail()
                    .await().indefinitely();
            assertEquals("reply_tl-req", result);
        } finally {
            reg.unregister();
        }
    }

    @Test
    public void testCreateWithTypeLiteralAndQualifiers() {
        List<String> received = new CopyOnWriteArrayList<>();

        var regDefault = receivers.newReceiver(Ping.class)
                .notify(ctx -> {
                    received.add("default_" + ctx.signal().id());
                });
        var regUrgent = receivers.newReceiver(Ping.class)
                .setQualifiers(Urgent.Literal.INSTANCE)
                .notify(ctx -> {
                    received.add("urgent_" + ctx.signal().id());
                });
        try {
            Signal<Ping> signal = Signal.create(new TypeLiteral<Ping>() {
            }, Urgent.Literal.INSTANCE);
            signal.publish(new Ping("tl-q"));
            Awaitility.await().until(() -> received.size() >= 1);
            assertEquals(1, received.size());
            assertTrue(received.contains("urgent_tl-q"),
                    "Only the @Urgent receiver should be invoked");
        } finally {
            regDefault.unregister();
            regUrgent.unregister();
        }
    }

    record Ping(String id) {
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
}
