package io.quarkus.signals.deployment.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.Dependent;
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
 * Verifies that a {@link Dependent} bean injected as a parameter of a receiver method
 * is destroyed after the notification completes.
 */
public class DependentParamDestroyedTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(
                    root -> root.addClasses(Receivers.class, DependentService.class, Cmd1.class, Cmd2.class));

    @Inject
    Signal<Cmd1> cmd1;

    @Inject
    Signal<Cmd2> cmd2;

    @Test
    public void testReceiverDestroysDependent() {
        DependentService.EVENTS.clear();
        cmd1.send(new Cmd1("blocking"));
        Awaitility.await().until(() -> DependentService.EVENTS.contains("destroyed"));
        assertEquals(2, DependentService.EVENTS.size());
        assertEquals("created", DependentService.EVENTS.get(0));
        assertEquals("destroyed", DependentService.EVENTS.get(1));

        DependentService.EVENTS.clear();
        String result = cmd2.reactive().request(new Cmd2("reactive"), String.class)
                .ifNoItem()
                .after(Duration.ofSeconds(1))
                .fail()
                .await().indefinitely();
        assertEquals("reactive", result);
        Awaitility.await().until(() -> DependentService.EVENTS.contains("destroyed"));
        assertEquals(2, DependentService.EVENTS.size());
        assertEquals("created", DependentService.EVENTS.get(0));
        assertEquals("destroyed", DependentService.EVENTS.get(1));
    }

    @Dependent
    public static class DependentService {

        static final List<String> EVENTS = new CopyOnWriteArrayList<>();

        public DependentService() {
        }

        void init() {
            EVENTS.add("created");
        }

        @PreDestroy
        void destroy() {
            EVENTS.add("destroyed");
        }
    }

    @Singleton
    public static class Receivers {

        void blocking(@Receives Cmd1 cmd, DependentService service) {
            if (!cmd.type().equals("blocking")) {
                throw new IllegalStateException();
            }
            service.init();
        }

        Uni<String> reactive(@Receives Cmd2 cmd, DependentService service) {
            service.init();
            return Uni.createFrom().item(cmd.type());
        }
    }

    record Cmd1(String type) {
    }

    record Cmd2(String type) {
    }
}
