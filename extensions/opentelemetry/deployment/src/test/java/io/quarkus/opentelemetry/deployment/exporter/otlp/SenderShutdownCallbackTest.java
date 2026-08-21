package io.quarkus.opentelemetry.deployment.exporter.otlp;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.OutputStream;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.sdk.common.export.MessageWriter;
import io.quarkus.opentelemetry.runtime.exporter.otlp.sender.VertxHttpSender;
import io.quarkus.test.QuarkusExtensionTest;
import io.vertx.core.Vertx;

/**
 * The exporter completes the result of an export in one of the two callbacks handed to the sender,
 * so the sender has to invoke exactly one of them. A sender that silently returns leaves the export
 * result uncompleted, and whoever waits for it — the batch processor draining its queue during
 * shutdown — waits until it times out, which keeps its worker thread, and the class loader that
 * thread pins, alive. See <a href="https://github.com/quarkusio/quarkus/issues/55614">issue #55614</a>.
 */
public class SenderShutdownCallbackTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

    @Inject
    Vertx vertx;

    @Test
    public void sendAfterShutdownInvokesTheErrorCallback() {
        VertxHttpSender sender = new VertxHttpSender(URI.create("http://localhost:4318"), "/v1/traces", false,
                Duration.ofSeconds(1), Map.of(), "application/x-protobuf", options -> {
                }, vertx);
        sender.shutdown();

        AtomicBoolean responded = new AtomicBoolean();
        AtomicReference<Throwable> error = new AtomicReference<>();
        sender.send(new EmptyMessageWriter(), response -> responded.set(true), error::set);

        assertNotNull(error.get(),
                "an export issued after the sender was shut down must fail, otherwise its result is never completed");
        assertTrue(!responded.get(), "the response callback must not be invoked for a shut down sender");
    }

    private static final class EmptyMessageWriter implements MessageWriter {

        @Override
        public void writeMessage(OutputStream output) {
        }

        @Override
        public int getContentLength() {
            return 0;
        }
    }
}
