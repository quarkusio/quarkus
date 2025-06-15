package io.quarkus.opentelemetry.runtime.exporter.otlp;

import java.io.ByteArrayOutputStream;

/**
 * Used when we know that the stream will never be used again, therefore we can skip copying the data WARNING: This
 * should only be used when we know that we will write at least this many bytes to the stream
 */
final class NonCopyingByteArrayOutputStream extends ByteArrayOutputStream {
    NonCopyingByteArrayOutputStream(int size) {
        super(size);
    }

    @Override
    public byte[] toByteArray() {
        return buf;
    }
}
