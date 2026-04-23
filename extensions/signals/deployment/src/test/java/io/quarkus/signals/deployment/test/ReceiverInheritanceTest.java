package io.quarkus.signals.deployment.test;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.signals.Receives;
import io.quarkus.signals.Signal;
import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.mutiny.Uni;

public class ReceiverInheritanceTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root.addClasses(
                    Msg.class,
                    BaseReceiver.class,
                    InheritingChild.class,
                    OverridingChild.class,
                    OverridingReceiverChild.class));

    @Inject
    Signal<Msg> msg;

    @Inject
    InheritingChild inheritingChild;

    @Inject
    OverridingChild overridingChild;

    @Inject
    OverridingReceiverChild overridingReceiverChild;

    @Test
    public void testReceiverInheritance() {
        inheritingChild.sequence.clear();
        overridingChild.sequence.clear();
        overridingReceiverChild.sequence.clear();

        Uni<Void> result = msg.publishUni(new Msg("test"));
        result.ifNoItem().after(Duration.ofSeconds(5)).fail().await().indefinitely();

        // InheritingChild inherits the receiver method from BaseReceiver
        assertThat(inheritingChild.sequence).containsExactly("parent_test");

        // OverridingChild overrides without @Receives - no receiver
        assertThat(overridingChild.sequence).isEmpty();

        // OverridingReceiverChild overrides with @Receives - uses child's implementation
        assertThat(overridingReceiverChild.sequence).containsExactly("child_test");
    }

    public record Msg(String value) {
    }

    public static abstract class BaseReceiver {
        final List<String> sequence = new CopyOnWriteArrayList<>();

        void onMsg(@Receives Msg msg) {
            sequence.add("parent_" + msg.value());
        }
    }

    @Singleton
    public static class InheritingChild extends BaseReceiver {
        // Inherits onMsg from BaseReceiver
    }

    @Singleton
    public static class OverridingChild extends BaseReceiver {
        @Override
        void onMsg(Msg msg) {
            sequence.add("override_" + msg.value());
        }
    }

    @Singleton
    public static class OverridingReceiverChild extends BaseReceiver {
        @Override
        void onMsg(@Receives Msg msg) {
            sequence.add("child_" + msg.value());
        }
    }
}
