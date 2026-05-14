package io.quarkus.signals.deployment.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.logging.LogRecord;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.signals.Receivers;
import io.quarkus.signals.Receivers.ExecutionModel;
import io.quarkus.signals.Signal;
import io.quarkus.signals.SignalContext;
import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.mutiny.Uni;

/**
 * Verifies that failures from fire-and-forget emission methods ({@link Signal#publish(Object)}
 * and {@link Signal#send(Object)}) are logged rather than propagated.
 */
public class BlockingEmissionFailureTest extends AbstractSignalTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root.addClasses(Cmd.class))
            .setLogRecordPredicate(r -> r.getLoggerName().contains("SignalImpl"))
            .assertLogRecords(records -> {
                assertFailureLogged(records, "publish-uni-boom");
                assertFailureLogged(records, "send-uni-boom");
            });

    @Inject
    Signal<Cmd> cmd;

    @Inject
    Receivers receivers;

    @Test
    public void testPublishUniFailure() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        var reg = receivers.newReceiver(Cmd.class)
                .setExecutionModel(ExecutionModel.NON_BLOCKING)
                .notify(new Function<SignalContext<Cmd>, Uni<Void>>() {
                    @Override
                    public Uni<Void> apply(SignalContext<Cmd> ctx) {
                        return Uni.createFrom().<Void> failure(new IllegalStateException("publish-uni-boom"))
                                .eventually(latch::countDown);
                    }
                });
        try {
            cmd.publish(new Cmd());
            assertTrue(latch.await(defaultTimeout().toMillis(), TimeUnit.MILLISECONDS));
        } finally {
            reg.unregister();
        }
    }

    @Test
    public void testSendUniFailure() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        var reg = receivers.newReceiver(Cmd.class)
                .setExecutionModel(ExecutionModel.NON_BLOCKING)
                .notify(new Function<SignalContext<Cmd>, Uni<Void>>() {
                    @Override
                    public Uni<Void> apply(SignalContext<Cmd> ctx) {
                        return Uni.createFrom().<Void> failure(new IllegalStateException("send-uni-boom"))
                                .eventually(latch::countDown);
                    }
                });
        try {
            cmd.send(new Cmd());
            assertTrue(latch.await(defaultTimeout().toMillis(), TimeUnit.MILLISECONDS));
        } finally {
            reg.unregister();
        }
    }

    private static void assertFailureLogged(List<LogRecord> records, String exceptionMessage) {
        boolean found = records.stream().anyMatch(r -> {
            if (r.getMessage().equals("Receiver notification failed: %s")
                    && r.getParameters() != null
                    && r.getParameters().length > 0) {
                return r.getParameters()[0].toString().contains(exceptionMessage);
            }
            return false;
        });
        assertThat(found)
                .as("Expected failure log containing: " + exceptionMessage)
                .isTrue();
    }

    record Cmd() {
    }
}
