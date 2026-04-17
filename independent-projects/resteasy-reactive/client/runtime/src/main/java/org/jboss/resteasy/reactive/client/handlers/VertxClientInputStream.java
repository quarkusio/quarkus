package org.jboss.resteasy.reactive.client.handlers;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Supplier;

import org.jboss.resteasy.reactive.common.core.BlockingNotAllowedException;

import io.netty.buffer.ByteBuf;
import io.quarkus.vertx.utils.VertxBlockingInput;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpHeaders;

public class VertxClientInputStream extends InputStream {

    private final VertxBlockingInput exchange;
    private final HttpClientResponse response;

    private boolean closed;
    private boolean finished;
    private ByteBuf pooled;

    public VertxClientInputStream(HttpClientResponse response, long timeout) {
        this.response = response;
        Runnable onTimeout = new Runnable() {
            @Override
            public void run() {
                response.netSocket().close();
            }
        };
        this.exchange = new VertxBlockingInput(response, timeout, onTimeout, false,
                new Supplier<RuntimeException>() {
                    @Override
                    public RuntimeException get() {
                        return new BlockingNotAllowedException("Attempting a blocking read on io thread");
                    }
                }, null);
    }

    @Override
    public int read() throws IOException {
        byte[] b = new byte[1];
        int read = read(b);
        if (read == -1) {
            return -1;
        }
        return b[0] & 0xff;
    }

    @Override
    public int read(final byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        if (closed) {
            throw new IOException("Stream is closed");
        }
        readIntoBuffer();
        if (finished) {
            return -1;
        }
        if (len == 0) {
            return 0;
        }
        ByteBuf buffer = pooled;
        int copied = Math.min(len, buffer.readableBytes());
        buffer.readBytes(b, off, copied);
        if (!buffer.isReadable()) {
            pooled.release();
            pooled = null;
        }
        return copied;
    }

    private void readIntoBuffer() throws IOException {
        if (pooled == null && !finished) {
            pooled = exchange.readBlocking();
            if (pooled == null) {
                finished = true;
                pooled = null;
            }
        }
    }

    @Override
    public int available() throws IOException {
        if (closed) {
            throw new IOException("Stream is closed");
        }
        if (finished) {
            return 0;
        }

        int buffered = exchange.readBytesAvailable();
        if (buffered > 0) {
            return buffered;
        }

        String length = response.getHeader(HttpHeaders.CONTENT_LENGTH);
        if (length == null) {
            return 0;
        }
        try {
            return Integer.parseInt(length);
        } catch (NumberFormatException e) {
            Long.parseLong(length); // ignore the value as can only return an int anyway
            return Integer.MAX_VALUE;
        }
    }

    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;
        try {
            if (!finished) {
                exchange.discard();
            }
        } finally {
            if (pooled != null) {
                pooled.release();
                pooled = null;
            }
            finished = true;
        }
    }

}
