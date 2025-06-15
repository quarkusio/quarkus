package io.quarkus.vertx.core.runtime;

import java.io.IOException;
import java.io.OutputStream;

import io.vertx.core.buffer.Buffer;

/**
 * Simple {@link OutputStream} implementation that appends content written in given {@link Buffer} instance.
 */
public class BufferOutputStream extends OutputStream {

    private final Buffer buffer;

    public BufferOutputStream(Buffer buffer) {
        this.buffer = buffer;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        buffer.appendBytes(b, off, len);
    }

    @Override
    public void write(int b) throws IOException {
        buffer.appendInt(b);
    }
}
