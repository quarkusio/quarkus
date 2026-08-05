package io.quarkus.signals.deployment.test;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Inject;
import jakarta.inject.Qualifier;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.signals.Receivers;
import io.quarkus.signals.Receivers.ExecutionModel;
import io.quarkus.signals.Receivers.ReceiverInfo;
import io.quarkus.signals.Receivers.ReceiverKind;
import io.quarkus.signals.Receives;
import io.quarkus.signals.SignalContext;
import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.mutiny.Uni;

public class ResolveReceiversTest extends AbstractSignalTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root.addClasses(
                    Event.class, Urgent.class, MyReceivers.class));

    @Inject
    Receivers receivers;

    @Test
    public void testDeclarativeReceivers() {
        List<ReceiverInfo> infos = receivers.resolveReceivers(Event.class);
        // [onEvent]
        assertThat(infos).hasSize(1);
        assertThat(infos).allMatch(i -> i.kind() == ReceiverKind.DECLARATIVE);
        assertThat(infos).allMatch(i -> i.signalType() == Event.class);
        assertThat(infos).allMatch(i -> i.responseType() == void.class);
    }

    @Test
    public void testProgrammaticReceiver() {
        Function<SignalContext<Event>, Uni<String>> callback = ctx -> Uni.createFrom().item("ok");
        Receivers.Registration reg = receivers.newReceiver(Event.class)
                .setExecutionModel(ExecutionModel.NON_BLOCKING)
                .notify(String.class, callback);
        try {
            List<ReceiverInfo> infos = receivers.resolveReceivers(Event.class);
            // [onEvent, reg]
            assertThat(infos).hasSize(2);
            assertThat(infos).anyMatch(i -> i.kind() == ReceiverKind.PROGRAMMATIC
                    && i.executionModel() == ExecutionModel.NON_BLOCKING
                    && i.responseType() == String.class);
        } finally {
            reg.unregister();
        }
    }

    @Test
    public void testQualifierFiltering() {
        // [onEvent]
        List<ReceiverInfo> unqualifiedReceivers = receivers.resolveReceivers(Event.class);
        // [onUrgentEvent]
        List<ReceiverInfo> urgentReceivers = receivers.resolveReceivers(Event.class, Urgent.Literal.INSTANCE);
        assertThat(urgentReceivers).hasSameSizeAs(unqualifiedReceivers).hasSize(1);
        // Only the @Urgent receiver has the @Urgent qualifier
        assertThat(urgentReceivers).allMatch(i -> i.qualifiers().stream()
                .anyMatch(q -> q.annotationType() == Urgent.class));
    }

    @Test
    public void testNoMatchingReceivers() {
        List<ReceiverInfo> infos = receivers.resolveReceivers(String.class);
        assertThat(infos).isEmpty();
    }

    @Test
    public void testProgrammaticReceiverUnregistered() {
        Consumer<SignalContext<Event>> noop = ctx -> {
        };
        Receivers.Registration reg = receivers.newReceiver(Event.class)
                .notify(noop);
        try {
            int sizeWithProgrammatic = receivers.resolveReceivers(Event.class).size();
            reg.unregister();
            int sizeAfterUnregister = receivers.resolveReceivers(Event.class).size();
            assertThat(sizeAfterUnregister).isLessThan(sizeWithProgrammatic);
        } finally {
            reg.unregister();
        }
    }

    record Event(String name) {
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
    public static class MyReceivers {

        void onEvent(@Receives Event event) {
        }

        Uni<String> onUrgentEvent(@Receives @Urgent SignalContext<Event> ctx) {
            return Uni.createFrom().item(ctx.signal().name());
        }
    }
}
