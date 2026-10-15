package io.quarkus.opentelemetry.deployment.exporter.otlp;

import java.io.OutputStream;

import io.opentelemetry.sdk.common.export.MessageWriter;

/**
 * A {@link MessageWriter} that writes an empty body.
 * <p>
 * The sender tests exercise the callback contract of {@code send()} and the retry behaviour of the
 * transport, neither of which depends on the exported payload, so this is the smallest writer that
 * satisfies the {@code send()} signature.
 */
final class EmptyMessageWriter implements MessageWriter {

    @Override
    public void writeMessage(OutputStream output) {
    }

    @Override
    public int getContentLength() {
        return 0;
    }
}
