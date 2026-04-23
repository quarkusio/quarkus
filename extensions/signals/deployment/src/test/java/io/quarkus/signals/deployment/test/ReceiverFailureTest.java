package io.quarkus.signals.deployment.test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
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
import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Uni;

/**
 * Verifies that exceptions thrown by receivers are propagated through the {@link Uni}
 * returned by {@link Signal.Emission#emit(Object)} for all execution models.
 * Tests both programmatic and declarative receivers.
 */
public class ReceiverFailureTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root.addClasses(Cmd.class,
                    WorkerCmd.class, EventLoopCmd.class,
                    DeclarativeReceivers.class));

    @Inject
    Signal<Cmd> cmd;

    @Inject
    Signal<WorkerCmd> workerSignal;

    @Inject
    Signal<EventLoopCmd> eventLoopSignal;

    @Inject
    Receivers receivers;

    @Test
    public void testPublishFailureWorkerThread() {
        var reg = receivers.newReceiver(Cmd.class)
                .setExecutionModel(ExecutionModel.BLOCKING)
                .notify(new Consumer<SignalContext<Cmd>>() {

                    @Override
                    public void accept(SignalContext<Cmd> t) {
                        throw new IllegalStateException("worker-boom");
                    }
                });
        try {
            var failure = assertThrows(CompositeException.class,
                    () -> cmd.publishUni(new Cmd("w"))
                            .ifNoItem().after(Duration.ofSeconds(5)).fail()
                            .await().indefinitely());
            assertInstanceOf(IllegalStateException.class, failure.getCauses().get(0));
        } finally {
            reg.unregister();
        }
    }

    @Test
    public void testPublishFailureEventLoop() {
        var reg = receivers.newReceiver(Cmd.class)
                .setExecutionModel(ExecutionModel.NON_BLOCKING)
                .notify(new Consumer<SignalContext<Cmd>>() {

                    @Override
                    public void accept(SignalContext<Cmd> t) {
                        throw new IllegalStateException("eventloop-boom");
                    }
                });
        try {
            var failure = assertThrows(CompositeException.class,
                    () -> cmd.publishUni(new Cmd("e"))
                            .ifNoItem().after(Duration.ofSeconds(5)).fail()
                            .await().indefinitely());
            assertInstanceOf(IllegalStateException.class, failure.getCauses().get(0));
        } finally {
            reg.unregister();
        }
    }

    @Test
    public void testSendFailureWorkerThread() {
        var reg = receivers.newReceiver(Cmd.class)
                .setExecutionModel(ExecutionModel.BLOCKING)
                .notify(new Consumer<SignalContext<Cmd>>() {

                    @Override
                    public void accept(SignalContext<Cmd> t) {
                        throw new IllegalStateException("worker-boom");
                    }
                });
        try {
            assertThrows(IllegalStateException.class,
                    () -> cmd.sendUni(new Cmd("w"))
                            .ifNoItem().after(Duration.ofSeconds(5)).fail()
                            .await().indefinitely());
        } finally {
            reg.unregister();
        }
    }

    @Test
    public void testSendFailureEventLoop() {
        var reg = receivers.newReceiver(Cmd.class)
                .setExecutionModel(ExecutionModel.NON_BLOCKING)
                .notify(new Consumer<SignalContext<Cmd>>() {

                    @Override
                    public void accept(SignalContext<Cmd> t) {
                        throw new IllegalStateException("eventloop-boom");
                    }
                });
        try {
            assertThrows(IllegalStateException.class,
                    () -> cmd.sendUni(new Cmd("e"))
                            .ifNoItem().after(Duration.ofSeconds(5)).fail()
                            .await().indefinitely());
        } finally {
            reg.unregister();
        }
    }

    @Test
    public void testRequestFailureWorkerThread() {
        var reg = receivers.newReceiver(Cmd.class)
                .setResponseType(String.class)
                .setExecutionModel(ExecutionModel.BLOCKING)
                .notify(new Function<SignalContext<Cmd>, Uni<String>>() {

                    @Override
                    public Uni<String> apply(SignalContext<Cmd> ctx) {
                        throw new IllegalStateException("worker-boom");
                    }
                });
        try {
            assertThrows(IllegalStateException.class,
                    () -> cmd.requestUni(new Cmd("w"), String.class)
                            .ifNoItem().after(Duration.ofSeconds(5)).fail()
                            .await().indefinitely());
        } finally {
            reg.unregister();
        }
    }

    @Test
    public void testRequestFailureEventLoop() {
        var reg = receivers.newReceiver(Cmd.class)
                .setResponseType(String.class)
                .setExecutionModel(ExecutionModel.NON_BLOCKING)
                .notify(new Function<SignalContext<Cmd>, Uni<String>>() {

                    @Override
                    public Uni<String> apply(SignalContext<Cmd> ctx) {
                        throw new IllegalStateException("eventloop-boom");
                    }
                });
        try {
            assertThrows(IllegalStateException.class,
                    () -> cmd.requestUni(new Cmd("e"), String.class)
                            .ifNoItem().after(Duration.ofSeconds(5)).fail()
                            .await().indefinitely());
        } finally {
            reg.unregister();
        }
    }

    @Test
    public void testDeclarativePublishFailureWorkerThread() {
        var failure = assertThrows(CompositeException.class,
                () -> workerSignal.publishUni(new WorkerCmd())
                        .ifNoItem().after(Duration.ofSeconds(5)).fail()
                        .await().indefinitely());
        assertInstanceOf(IllegalStateException.class, failure.getCauses().get(0));
    }

    @Test
    public void testDeclarativePublishFailureEventLoop() {
        var failure = assertThrows(CompositeException.class,
                () -> eventLoopSignal.publishUni(new EventLoopCmd())
                        .ifNoItem().after(Duration.ofSeconds(5)).fail()
                        .await().indefinitely());
        assertInstanceOf(IllegalStateException.class, failure.getCauses().get(0));
    }

    @Test
    public void testDeclarativeSendFailureWorkerThread() {
        assertThrows(IllegalStateException.class,
                () -> workerSignal.sendUni(new WorkerCmd())
                        .ifNoItem().after(Duration.ofSeconds(5)).fail()
                        .await().indefinitely());
    }

    @Test
    public void testDeclarativeSendFailureEventLoop() {
        assertThrows(IllegalStateException.class,
                () -> eventLoopSignal.sendUni(new EventLoopCmd())
                        .ifNoItem().after(Duration.ofSeconds(5)).fail()
                        .await().indefinitely());
    }

    record Cmd(String id) {
    }

    record WorkerCmd() {
    }

    record EventLoopCmd() {
    }

    @Singleton
    public static class DeclarativeReceivers {

        void onWorker(@Receives WorkerCmd cmd) {
            throw new IllegalStateException("declarative-worker-boom");
        }

        Uni<Void> onEventLoop(@Receives EventLoopCmd cmd) {
            throw new IllegalStateException("declarative-eventloop-boom");
        }
    }
}
