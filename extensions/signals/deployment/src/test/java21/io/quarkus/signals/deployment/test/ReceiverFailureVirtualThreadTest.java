package io.quarkus.signals.deployment.test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.function.Consumer;
import java.util.function.Function;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.signals.Receivers;
import io.quarkus.signals.Receives;
import io.quarkus.signals.Signal;
import io.quarkus.signals.SignalContext;
import io.quarkus.signals.spi.Receiver.ExecutionModel;
import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Uni;

/**
 * Virtual-thread-specific failure tests extracted from {@link ReceiverFailureTest}.
 */
public class ReceiverFailureVirtualThreadTest extends AbstractSignalTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root.addClasses(Cmd.class, VirtualCmd.class, DeclarativeReceivers.class));

    @Inject
    Signal<Cmd> cmd;

    @Inject
    Signal<VirtualCmd> virtualSignal;

    @Inject
    Receivers receivers;

    @Test
    public void testPublishFailureVirtualThread() {
        var reg = receivers.newReceiver(Cmd.class)
                .setExecutionModel(ExecutionModel.VIRTUAL_THREAD)
                .notify(new Consumer<SignalContext<Cmd>>() {

                    @Override
                    public void accept(SignalContext<Cmd> t) {
                        throw new IllegalStateException("virtual-boom");
                    }
                });
        try {
            var failure = assertThrows(CompositeException.class,
                    () -> cmd.reactive().publish(new Cmd("v"))
                            .ifNoItem().after(defaultTimeout()).fail()
                            .await().indefinitely());
            assertInstanceOf(IllegalStateException.class, failure.getCauses().get(0));
        } finally {
            reg.unregister();
        }
    }

    @Test
    public void testSendFailureVirtualThread() {
        var reg = receivers.newReceiver(Cmd.class)
                .setExecutionModel(ExecutionModel.VIRTUAL_THREAD)
                .notify(new Consumer<SignalContext<Cmd>>() {

                    @Override
                    public void accept(SignalContext<Cmd> t) {
                        throw new IllegalStateException("virtual-boom");
                    }
                });
        try {
            assertThrows(IllegalStateException.class,
                    () -> cmd.reactive().send(new Cmd("v"))
                            .ifNoItem().after(defaultTimeout()).fail()
                            .await().indefinitely());
        } finally {
            reg.unregister();
        }
    }

    @Test
    public void testRequestFailureVirtualThread() {
        var reg = receivers.newReceiver(Cmd.class)
                .setResponseType(String.class)
                .setExecutionModel(ExecutionModel.VIRTUAL_THREAD)
                .notify(new Function<SignalContext<Cmd>, Uni<String>>() {

                    @Override
                    public Uni<String> apply(SignalContext<Cmd> ctx) {
                        throw new IllegalStateException("virtual-boom");
                    }
                });
        try {
            assertThrows(IllegalStateException.class,
                    () -> cmd.reactive().request(new Cmd("v"), String.class)
                            .ifNoItem().after(defaultTimeout()).fail()
                            .await().indefinitely());
        } finally {
            reg.unregister();
        }
    }

    @Test
    public void testDeclarativePublishFailureVirtualThread() {
        var failure = assertThrows(CompositeException.class,
                () -> virtualSignal.reactive().publish(new VirtualCmd())
                        .ifNoItem().after(defaultTimeout()).fail()
                        .await().indefinitely());
        assertInstanceOf(IllegalStateException.class, failure.getCauses().get(0));
    }

    @Test
    public void testDeclarativeSendFailureVirtualThread() {
        assertThrows(IllegalStateException.class,
                () -> virtualSignal.reactive().send(new VirtualCmd())
                        .ifNoItem().after(defaultTimeout()).fail()
                        .await().indefinitely());
    }

    record Cmd(String id) {
    }

    record VirtualCmd() {
    }

    @Singleton
    public static class DeclarativeReceivers {

        @RunOnVirtualThread
        void onVirtual(@Receives VirtualCmd cmd) {
            throw new IllegalStateException("declarative-virtual-boom");
        }
    }
}
