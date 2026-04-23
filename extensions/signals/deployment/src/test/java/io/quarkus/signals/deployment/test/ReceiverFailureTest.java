package io.quarkus.signals.deployment.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
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
public class ReceiverFailureTest extends AbstractSignalTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root.addClasses(Cmd.class,
                    WorkerCmd.class, EventLoopCmd.class, UniFailureCmd.class,
                    DeclarativeReceivers.class));

    @Inject
    Signal<Cmd> cmd;

    @Inject
    Signal<WorkerCmd> workerSignal;

    @Inject
    Signal<EventLoopCmd> eventLoopSignal;

    @Inject
    Signal<UniFailureCmd> uniFailureSignal;

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
                    () -> cmd.reactive().publish(new Cmd("w"))
                            .ifNoItem().after(defaultTimeout()).fail()
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
                    () -> cmd.reactive().publish(new Cmd("e"))
                            .ifNoItem().after(defaultTimeout()).fail()
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
                    () -> cmd.reactive().send(new Cmd("w"))
                            .ifNoItem().after(defaultTimeout()).fail()
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
                    () -> cmd.reactive().send(new Cmd("e"))
                            .ifNoItem().after(defaultTimeout()).fail()
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
                    () -> cmd.reactive().request(new Cmd("w"), String.class)
                            .ifNoItem().after(defaultTimeout()).fail()
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
                    () -> cmd.reactive().request(new Cmd("e"), String.class)
                            .ifNoItem().after(defaultTimeout()).fail()
                            .await().indefinitely());
        } finally {
            reg.unregister();
        }
    }

    @Test
    public void testBlockingRequestFailure() {
        var reg = receivers.newReceiver(Cmd.class)
                .setResponseType(String.class)
                .setExecutionModel(ExecutionModel.BLOCKING)
                .notify(new Function<SignalContext<Cmd>, Uni<String>>() {

                    @Override
                    public Uni<String> apply(SignalContext<Cmd> ctx) {
                        throw new IllegalStateException("blocking-request-boom");
                    }
                });
        try {
            assertThrows(IllegalStateException.class,
                    () -> cmd.request(new Cmd("br"), String.class));
        } finally {
            reg.unregister();
        }
    }

    @Test
    public void testBlockingRequestCheckedExceptionWrapped() {
        var reg = receivers.newReceiver(Cmd.class)
                .setResponseType(String.class)
                .setExecutionModel(ExecutionModel.BLOCKING)
                .notify(new Function<SignalContext<Cmd>, Uni<String>>() {

                    @Override
                    public Uni<String> apply(SignalContext<Cmd> ctx) {
                        return Uni.createFrom().failure(new Exception("checked-boom"));
                    }
                });
        try {
            var cex = assertThrows(java.util.concurrent.CompletionException.class,
                    () -> cmd.request(new Cmd("ce"), String.class));
            assertInstanceOf(Exception.class, cex.getCause());
            assertEquals("checked-boom", cex.getCause().getMessage());
        } finally {
            reg.unregister();
        }
    }

    @Test
    public void testDeclarativePublishFailureWorkerThread() {
        var failure = assertThrows(CompositeException.class,
                () -> workerSignal.reactive().publish(new WorkerCmd())
                        .ifNoItem().after(defaultTimeout()).fail()
                        .await().indefinitely());
        assertInstanceOf(IllegalStateException.class, failure.getCauses().get(0));
    }

    @Test
    public void testDeclarativePublishFailureEventLoop() {
        var failure = assertThrows(CompositeException.class,
                () -> eventLoopSignal.reactive().publish(new EventLoopCmd())
                        .ifNoItem().after(defaultTimeout()).fail()
                        .await().indefinitely());
        assertInstanceOf(IllegalStateException.class, failure.getCauses().get(0));
    }

    @Test
    public void testDeclarativeSendFailureWorkerThread() {
        assertThrows(IllegalStateException.class,
                () -> workerSignal.reactive().send(new WorkerCmd())
                        .ifNoItem().after(defaultTimeout()).fail()
                        .await().indefinitely());
    }

    @Test
    public void testDeclarativeSendFailureEventLoop() {
        assertThrows(IllegalStateException.class,
                () -> eventLoopSignal.reactive().send(new EventLoopCmd())
                        .ifNoItem().after(defaultTimeout()).fail()
                        .await().indefinitely());
    }

    @Test
    public void testPublishPartialFailure() {
        List<String> received = new CopyOnWriteArrayList<>();

        // One receiver succeeds
        var regOk = receivers.newReceiver(Cmd.class)
                .notify(new Consumer<SignalContext<Cmd>>() {
                    @Override
                    public void accept(SignalContext<Cmd> ctx) {
                        received.add("ok_" + ctx.signal().id());
                    }
                });
        // Another receiver fails
        var regFail = receivers.newReceiver(Cmd.class)
                .notify(new Consumer<SignalContext<Cmd>>() {
                    @Override
                    public void accept(SignalContext<Cmd> ctx) {
                        throw new IllegalStateException("partial-boom");
                    }
                });
        try {
            var failure = assertThrows(CompositeException.class,
                    () -> cmd.reactive().publish(new Cmd("partial"))
                            .ifNoItem().after(defaultTimeout()).fail()
                            .await().indefinitely());
            // The successful receiver should still have been invoked
            assertEquals(1, received.size());
            assertEquals("ok_partial", received.get(0));
            // The composite exception should contain exactly one cause
            assertEquals(1, failure.getCauses().size());
            assertInstanceOf(IllegalStateException.class, failure.getCauses().get(0));
        } finally {
            regOk.unregister();
            regFail.unregister();
        }
    }

    @Test
    public void testPublishUniFailure() {
        var reg = receivers.newReceiver(Cmd.class)
                .setExecutionModel(ExecutionModel.NON_BLOCKING)
                .notify(new Function<SignalContext<Cmd>, Uni<Void>>() {
                    @Override
                    public Uni<Void> apply(SignalContext<Cmd> ctx) {
                        return Uni.createFrom().failure(new IllegalStateException("publish-uni-boom"));
                    }
                });
        try {
            var failure = assertThrows(CompositeException.class,
                    () -> cmd.reactive().publish(new Cmd("u"))
                            .ifNoItem().after(defaultTimeout()).fail()
                            .await().indefinitely());
            assertInstanceOf(IllegalStateException.class, failure.getCauses().get(0));
        } finally {
            reg.unregister();
        }
    }

    @Test
    public void testSendUniFailure() {
        var reg = receivers.newReceiver(Cmd.class)
                .setExecutionModel(ExecutionModel.NON_BLOCKING)
                .notify(new Function<SignalContext<Cmd>, Uni<Void>>() {
                    @Override
                    public Uni<Void> apply(SignalContext<Cmd> ctx) {
                        return Uni.createFrom().failure(new IllegalStateException("send-uni-boom"));
                    }
                });
        try {
            assertThrows(IllegalStateException.class,
                    () -> cmd.reactive().send(new Cmd("u"))
                            .ifNoItem().after(defaultTimeout()).fail()
                            .await().indefinitely());
        } finally {
            reg.unregister();
        }
    }

    @Test
    public void testRequestUniFailure() {
        var reg = receivers.newReceiver(Cmd.class)
                .setResponseType(String.class)
                .setExecutionModel(ExecutionModel.NON_BLOCKING)
                .notify(new Function<SignalContext<Cmd>, Uni<String>>() {
                    @Override
                    public Uni<String> apply(SignalContext<Cmd> ctx) {
                        return Uni.createFrom().failure(new IllegalStateException("request-uni-boom"));
                    }
                });
        try {
            assertThrows(IllegalStateException.class,
                    () -> cmd.reactive().request(new Cmd("u"), String.class)
                            .ifNoItem().after(defaultTimeout()).fail()
                            .await().indefinitely());
        } finally {
            reg.unregister();
        }
    }

    @Test
    public void testDeclarativePublishUniFailure() {
        var failure = assertThrows(CompositeException.class,
                () -> uniFailureSignal.reactive().publish(new UniFailureCmd())
                        .ifNoItem().after(defaultTimeout()).fail()
                        .await().indefinitely());
        assertInstanceOf(IllegalStateException.class, failure.getCauses().get(0));
    }

    @Test
    public void testDeclarativeSendUniFailure() {
        assertThrows(IllegalStateException.class,
                () -> uniFailureSignal.reactive().send(new UniFailureCmd())
                        .ifNoItem().after(defaultTimeout()).fail()
                        .await().indefinitely());
    }

    record Cmd(String id) {
    }

    record WorkerCmd() {
    }

    record EventLoopCmd() {
    }

    record UniFailureCmd() {
    }

    @Singleton
    public static class DeclarativeReceivers {

        void onWorker(@Receives WorkerCmd cmd) {
            throw new IllegalStateException("declarative-worker-boom");
        }

        Uni<Void> onEventLoop(@Receives EventLoopCmd cmd) {
            throw new IllegalStateException("declarative-eventloop-boom");
        }

        Uni<Void> onUniFailure(@Receives UniFailureCmd cmd) {
            return Uni.createFrom().failure(new IllegalStateException("declarative-uni-failure-boom"));
        }
    }
}
