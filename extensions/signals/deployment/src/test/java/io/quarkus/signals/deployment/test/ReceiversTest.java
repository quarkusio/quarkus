package io.quarkus.signals.deployment.test;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Inject;
import jakarta.inject.Qualifier;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.signals.Receivers;
import io.quarkus.signals.Signal;
import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.mutiny.Uni;

/**
 * Verifies programmatic registration and unregistration of receivers via {@link Receivers}.
 */
public class ReceiversTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root.addClasses(Order.class, Priority.class, RequestBean.class));

    @Inject
    Signal<Order> order;

    @Inject
    @Any
    Signal<Order> anyOrder;

    @Inject
    Receivers receivers;

    @Inject
    RequestBean requestBean;

    @Test
    public void testRegisterAndUnregister() {
        List<String> received = new CopyOnWriteArrayList<>();

        // Register a custom receiver
        var reg = receivers.newReceiver(Order.class)
                .notify(ctx -> {
                    received.add("listener_" + ctx.signal().id());
                });

        // Emit — the registered receiver should be invoked
        order.publish(new Order("1"));
        Awaitility.await().until(() -> received.size() >= 1);
        assertEquals(1, received.size());
        assertEquals("listener_1", received.get(0));

        // Unregister
        reg.unregister();
        received.clear();

        // Emit again — the receiver should no longer be invoked
        order.publish(new Order("2"));
        // Give some time for potential (unwanted) delivery
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        assertTrue(received.isEmpty(), "Receiver should not be invoked after unregister");
    }

    @Test
    public void testRegisterWithResponse() {
        Receivers.Registration reg = receivers.newReceiver(Order.class)
                .setResponseType(String.class)
                .notify(ctx -> {
                    return Uni.createFrom().item("processed_" + ctx.signal().id());
                });

        String result = order.reactive().request(new Order("42"), String.class)
                .ifNoItem()
                .after(Duration.ofSeconds(1))
                .fail()
                .await().indefinitely();
        assertEquals("processed_42", result);

        reg.unregister();
    }

    @Test
    public void testSetQualifiers() {
        List<String> received = new CopyOnWriteArrayList<>();

        // Register a receiver with @Priority qualifier
        var reg = receivers.newReceiver(Order.class)
                .setQualifiers(Priority.Literal.INSTANCE)
                .notify(ctx -> {
                    received.add("priority_" + ctx.signal().id());
                });

        // Unqualified publish — should NOT reach the @Priority receiver
        anyOrder.publish(new Order("1"));
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        assertTrue(received.isEmpty(), "Qualified receiver should not receive unqualified signal");

        // Qualified publish — should reach the @Priority receiver
        anyOrder.select(Priority.Literal.INSTANCE).publish(new Order("2"));
        Awaitility.await().until(() -> received.size() >= 1);
        assertEquals(1, received.size());
        assertEquals("priority_2", received.get(0));

        reg.unregister();
    }

    @Test
    public void testSetQualifiersInvalid() {
        // @SuppressWarnings is not a qualifier — should throw
        assertThrows(IllegalArgumentException.class,
                () -> receivers.newReceiver(Order.class)
                        .setQualifiers(new SuppressWarnings() {
                            @Override
                            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                                return SuppressWarnings.class;
                            }

                            @Override
                            public String[] value() {
                                return new String[0];
                            }
                        }));
    }

    @Test
    public void testRequestContextActive() {
        AtomicBoolean requestContextActive = new AtomicBoolean();

        var reg = receivers.newReceiver(Order.class)
                .setResponseType(String.class)
                .notify(ctx -> {
                    requestContextActive.set(Arc.container().requestContext().isActive());
                    // Verify that a @RequestScoped bean can be used
                    requestBean.ping();
                    return Uni.createFrom().item("ok");
                });

        String result = order.reactive().request(new Order("req-ctx"), String.class)
                .ifNoItem().after(Duration.ofSeconds(5)).fail()
                .await().indefinitely();

        assertEquals("ok", result);
        assertTrue(requestContextActive.get(), "Request context must be active during programmatic receiver notification");

        reg.unregister();
    }

    record Order(String id) {
    }

    @RequestScoped
    public static class RequestBean {
        public void ping() {
        }
    }

    @Qualifier
    @Target({ FIELD, METHOD, PARAMETER })
    @Retention(RUNTIME)
    public @interface Priority {

        final class Literal extends AnnotationLiteral<Priority> implements Priority {
            public static final Literal INSTANCE = new Literal();
            private static final long serialVersionUID = 1L;
        }
    }
}
