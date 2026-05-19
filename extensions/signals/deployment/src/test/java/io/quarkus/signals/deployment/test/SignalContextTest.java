package io.quarkus.signals.deployment.test;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Set;
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
import io.quarkus.signals.SignalContext;
import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.mutiny.Uni;

/**
 * Verifies that {@link SignalContext} accessors return correct values for each emission type.
 */
public class SignalContextTest extends AbstractSignalTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root.addClasses(MyReceivers.class, Cmd.class, Priority.class));

    @Inject
    @Any
    Signal<Cmd> signal;

    @Inject
    MyReceivers myReceivers;

    @Test
    public void testPublishEmissionType() {
        myReceivers.contexts.clear();

        signal.publish(new Cmd("pub"));
        Awaitility.await().until(() -> myReceivers.contexts.size() >= 1);

        SignalContext<?> ctx = myReceivers.contexts.get(0);
        assertEquals(SignalContext.EmissionType.PUBLISH, ctx.emissionType());
        assertNull(ctx.responseType());
    }

    @Test
    public void testSendEmissionType() {
        myReceivers.contexts.clear();

        signal.send(new Cmd("snd"));
        Awaitility.await().until(() -> myReceivers.contexts.size() >= 1);

        SignalContext<?> ctx = myReceivers.contexts.get(0);
        assertEquals(SignalContext.EmissionType.SEND, ctx.emissionType());
        assertNull(ctx.responseType());
    }

    @Test
    public void testRequestEmissionType() {
        myReceivers.contexts.clear();

        signal.select(Priority.Literal.INSTANCE).reactive().request(new Cmd("req"), String.class)
                .ifNoItem().after(defaultTimeout()).fail()
                .await().indefinitely();

        SignalContext<?> ctx = myReceivers.contexts.get(0);
        assertEquals(SignalContext.EmissionType.REQUEST, ctx.emissionType());
        assertEquals(String.class, ctx.responseType());
    }

    @Test
    public void testSignalType() {
        myReceivers.contexts.clear();

        signal.publish(new Cmd("type-check"));
        Awaitility.await().until(() -> myReceivers.contexts.size() >= 1);

        SignalContext<?> ctx = myReceivers.contexts.get(0);
        assertEquals(Cmd.class, ctx.signalType());
    }

    @Test
    public void testQualifiers() {
        myReceivers.contexts.clear();

        signal.select(Priority.Literal.INSTANCE).publish(new Cmd("qual"));
        Awaitility.await().until(() -> myReceivers.contexts.size() >= 1);

        SignalContext<?> ctx = myReceivers.contexts.get(0);
        Set<Annotation> qualifiers = ctx.qualifiers();
        assertNotNull(qualifiers);
        assertTrue(qualifiers.stream().anyMatch(a -> a.annotationType().equals(Priority.class)),
                "Qualifiers should contain @Priority");
    }

    @Singleton
    public static class MyReceivers {

        final CopyOnWriteArrayList<SignalContext<?>> contexts = new CopyOnWriteArrayList<>();

        void observe(@Receives @Any SignalContext<Cmd> ctx) {
            contexts.add(ctx);
        }

        Uni<String> respond(@Receives @Any @Priority SignalContext<Cmd> ctx) {
            contexts.add(ctx);
            return Uni.createFrom().item(ctx.signal().value());
        }
    }

    record Cmd(String value) {
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
