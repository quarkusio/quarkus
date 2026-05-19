package io.quarkus.signals.deployment.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.util.TypeLiteral;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.signals.Receives;
import io.quarkus.signals.Signal;
import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.mutiny.Uni;

/**
 * Verifies that {@link Signal#select(Class, java.lang.annotation.Annotation...)} narrows
 * the signal type to a subtype for receiver matching.
 */
public class SignalSelectSubtypeTest extends AbstractSignalTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(
                    root -> root.addClasses(Receivers.class, TaskEvent.class, TaskCreated.class, TaskCompleted.class));

    @Inject
    Signal<TaskEvent> taskEvent;

    @Inject
    Receivers receivers;

    @Test
    public void testSelectSubtype() {
        receivers.sequence.clear();

        // Select TaskCreated subtype — should reach only the TaskCreated receiver
        taskEvent.select(TaskCreated.class).publish(new TaskCreated("build"));
        Awaitility.await().until(() -> receivers.sequence.size() >= 1);
        assertEquals(1, receivers.sequence.size());
        assertEquals("created_build", receivers.sequence.get(0));

        receivers.sequence.clear();

        // Select TaskCompleted subtype — should reach only the TaskCompleted receiver
        Uni<String> uni = taskEvent.select(TaskCompleted.class)
                .reactive().request(new TaskCompleted("deploy"), String.class);
        String result = uni.ifNoItem()
                .after(defaultTimeout())
                .fail()
                .await().indefinitely();
        assertEquals("DEPLOY", result);
        assertEquals(1, receivers.sequence.size());
        assertEquals("completed_deploy", receivers.sequence.get(0));
    }

    @Test
    public void testSelectSubtypeWithTypeLiteral() {
        receivers.sequence.clear();

        Uni<String> uni = taskEvent.select(new TypeLiteral<TaskCompleted>() {
        }).reactive().request(new TaskCompleted("release"), String.class);
        String result = uni.ifNoItem()
                .after(defaultTimeout())
                .fail()
                .await().indefinitely();
        assertEquals("RELEASE", result);
        assertEquals(1, receivers.sequence.size());
        assertEquals("completed_release", receivers.sequence.get(0));
    }

    @Singleton
    public static class Receivers {

        final List<String> sequence = new CopyOnWriteArrayList<>();

        void onCreated(@Receives TaskCreated event) {
            sequence.add("created_" + event.name());
        }

        Uni<String> onCompleted(@Receives TaskCompleted event) {
            sequence.add("completed_" + event.name());
            return Uni.createFrom().item(event.name().toUpperCase());
        }
    }

    public static class TaskEvent {
        private final String name;

        public TaskEvent(String name) {
            this.name = name;
        }

        public String name() {
            return name;
        }
    }

    public static class TaskCreated extends TaskEvent {
        public TaskCreated(String name) {
            super(name);
        }
    }

    public static class TaskCompleted extends TaskEvent {
        public TaskCompleted(String name) {
            super(name);
        }
    }
}
