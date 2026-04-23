package io.quarkus.signals.deployment.test;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Inject;
import jakarta.inject.Qualifier;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.signals.Receives;
import io.quarkus.signals.Signal;
import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.mutiny.Uni;

/**
 * Verifies that {@link Signal#select(java.lang.annotation.Annotation...)} narrows
 * the set of matching receivers at runtime.
 */
public class SignalSelectTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root.addClasses(Receivers.class, Msg.class, Urgent.class));

    @Inject
    @Any
    Signal<Msg> msg;

    @Inject
    Receivers receivers;

    @Test
    public void testSelect() {
        receivers.sequence.clear();

        // select @Urgent — should reach only the qualified receiver
        Uni<String> uni = msg.select(Urgent.Literal.INSTANCE)
                .requestUni(new Msg("fire"), String.class);
        String result = uni.ifNoItem()
                .after(Duration.ofSeconds(1))
                .fail()
                .await().indefinitely();
        assertEquals("FIRE", result);
        assertEquals(1, receivers.sequence.size());
        assertEquals("urgent_fire", receivers.sequence.get(0));
    }

    @Singleton
    public static class Receivers {

        final List<String> sequence = new CopyOnWriteArrayList<>();

        void general(@Receives Msg msg) {
            sequence.add("general_" + msg.text());
        }

        Uni<String> urgent(@Receives @Urgent Msg msg) {
            sequence.add("urgent_" + msg.text());
            return Uni.createFrom().item(msg.text().toUpperCase());
        }
    }

    record Msg(String text) {
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
