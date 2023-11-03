package org.jboss.resteasy.reactive.client.impl;

import java.io.IOException;
import java.io.OutputStream;

import io.vertx.core.buffer.Buffer;

public class VertxBufferOutputStream extends OutputStream {
    private final Buffer buffer;

    public VertxBufferOutputStream() {
        this.buffer = Buffer.buffer();
    }

    @Override
    public void write(int b) throws IOException {
        buffer.appendByte((byte) (b & 0xFF));
    }

    @Override
    public void write(byte[] b) throws IOException {
        buffer.appendBytes(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        buffer.appendBytes(b, off, len);
    }

    public Buffer getBuffer() {
        return this.buffer.copy();
    }
}
