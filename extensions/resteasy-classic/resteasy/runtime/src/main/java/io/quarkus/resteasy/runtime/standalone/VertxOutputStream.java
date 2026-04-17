package io.quarkus.resteasy.runtime.standalone;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import jakarta.ws.rs.core.HttpHeaders;

import org.jboss.resteasy.spi.AsyncOutputStream;

import io.netty.buffer.ByteBuf;
import io.quarkus.vertx.utils.AppendBuffer;

public class VertxOutputStream extends AsyncOutputStream {

    private final VertxHttpResponse response;
    private final AppendBuffer appendBuffer;
    private long written;
    private final long contentLength;

    private boolean closed;

    public VertxOutputStream(VertxHttpResponse response, int bufferSize) {
        this.appendBuffer = AppendBuffer.eager(bufferSize);
        this.response = response;
        Object length = response.getOutputHeaders().getFirst(HttpHeaders.CONTENT_LENGTH);
        this.contentLength = length == null ? -1 : Long.parseLong(length.toString());
    }

    /**
     * {@inheritDoc}
     */
    public void write(final int b) throws IOException {
        write(new byte[] { (byte) b }, 0, 1);
    }

    /**
     * {@inheritDoc}
     */
    public void write(final byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    /**
     * {@inheritDoc}
     */
    public void write(final byte[] b, final int off, final int len) throws IOException {
        if (len < 1) {
            return;
        }
        if (closed) {
            throw new IOException("Stream is closed");
        }
        try {
            appendBuffer.appendAndDrain(b, off, len, new AppendBuffer.DrainHandler() {
                @Override
                public void drain(ByteBuf buffer, boolean finished) throws IOException {
                    response.writeBlocking(buffer, finished);
                }
            });
        } catch (Exception e) {
            ByteBuf leftover = appendBuffer.clear();
            if (leftover != null) {
                leftover.release();
            }
            closed = true;
            throw new IOException(e);
        }
        updateWritten(len);
    }

    void updateWritten(final long len) throws IOException {
        this.written += len;
        if (contentLength != -1 && this.written >= contentLength) {
            flush();
            close();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void flush() throws IOException {
        if (closed) {
            throw new IOException("Stream is closed");
        }
        try {
            ByteBuf buf = appendBuffer.clear();
            if (buf != null) {
                response.writeBlocking(buf, false);
            }
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void close() throws IOException {
        if (closed)
            return;
        try {
            response.writeBlocking(appendBuffer.clear(), true);
        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            closed = true;
        }
    }

    @Override
    public CompletionStage<Void> asyncFlush() {
        return asyncFlush(false);
    }

    private CompletionStage<Void> asyncFlush(boolean isLast) {
        if (closed) {
            CompletableFuture<Void> ret = new CompletableFuture<>();
            ret.completeExceptionally(new IOException("Stream is closed"));
            return ret;
        }
        ByteBuf buf = appendBuffer.clear();
        if (buf != null) {
            return response.writeNonBlocking(buf, isLast);
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<Void> asyncWrite(final byte[] b, final int off, final int len) {
        if (len < 1) {
            return CompletableFuture.completedFuture(null);
        }
        if (closed) {
            CompletableFuture<Void> ret = new CompletableFuture<>();
            ret.completeExceptionally(new IOException("Stream is closed"));
            return ret;
        }
        return asyncDrain(b, off, len)
                .thenCompose(new Function<Void, CompletionStage<Void>>() {
                    @Override
                    public CompletionStage<Void> apply(Void v) {
                        return VertxOutputStream.this.asyncUpdateWritten(len);
                    }
                });
    }

    private CompletionStage<Void> asyncDrain(byte[] b, int off, int len) {
        int written = appendBuffer.append(b, off, len);
        if (written < len) {
            ByteBuf buf = appendBuffer.clear();
            int newOff = off + written;
            int newLen = len - written;
            return response.writeNonBlocking(buf, false)
                    .thenCompose(new Function<Void, CompletionStage<Void>>() {
                        @Override
                        public CompletionStage<Void> apply(Void v) {
                            return VertxOutputStream.this.asyncDrain(b, newOff, newLen);
                        }
                    });
        }
        return CompletableFuture.completedFuture(null);
    }

    CompletionStage<Void> asyncUpdateWritten(final long len) {
        this.written += len;
        if (contentLength != -1 && this.written >= contentLength) {
            return asyncFlush(true).thenAccept(v -> {
                closed = true;
            });
        }
        return CompletableFuture.completedFuture(null);
    }
}
