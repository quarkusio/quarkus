package io.quarkus.resteasy.runtime.standalone;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.ws.rs.core.HttpHeaders;

import org.jboss.resteasy.spi.AsyncOutputStream;

import io.netty.buffer.ByteBuf;

public class VertxOutputStream extends AsyncOutputStream {

    private final VertxHttpResponse response;
    private final BufferAllocator allocator;
    private ByteBuf pooledBuffer;
    private long written;
    private final long contentLength;

    private boolean closed;

    /**
     * Construct a new instance. No write timeout is configured.
     *
     */
    public VertxOutputStream(VertxHttpResponse response, BufferAllocator allocator) {
        this.allocator = allocator;
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

        int rem = len;
        int idx = off;
        ByteBuf buffer = pooledBuffer;
        try {
            if (buffer == null) {
                pooledBuffer = buffer = allocator.allocateBuffer();
            }
            while (rem > 0) {
                int toWrite = Math.min(rem, buffer.writableBytes());
                buffer.writeBytes(b, idx, toWrite);
                rem -= toWrite;
                idx += toWrite;
                if (!buffer.isWritable()) {
                    ByteBuf tmpBuf = buffer;
                    this.pooledBuffer = buffer = allocator.allocateBuffer();
                    response.writeBlocking(tmpBuf, false);
                }
            }
        } catch (Exception e) {
            if (buffer != null && buffer.refCnt() > 0) {
                buffer.release();
            }
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
            if (pooledBuffer != null) {
                response.writeBlocking(pooledBuffer, false);
                pooledBuffer = null;
            }
        } catch (Exception e) {
            if (pooledBuffer != null) {
                pooledBuffer.release();
                pooledBuffer = null;
            }
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
            response.writeBlocking(pooledBuffer, true);
        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            closed = true;
            pooledBuffer = null;
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
        if (pooledBuffer != null) {
            ByteBuf sentBuffer = pooledBuffer;
            pooledBuffer = null;
            return response.writeNonBlocking(sentBuffer, isLast);
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

        CompletionStage<Void> ret = CompletableFuture.completedFuture(null);
        int bufferSize = allocator.getBufferSize();
        int bufferCount = len / bufferSize;
        int remainder = len % bufferSize;
        if (remainder != 0) {
            bufferCount = bufferCount + 1;
        }

        if (bufferCount == 1) {
            pooledBuffer = allocator.allocateBuffer();
            pooledBuffer.writeBytes(b);
        } else {
            for (int i = 0; i < bufferCount - 1; i++) {
                int bufferIndex = i;
                ret = ret.thenCompose(v -> {
                    ByteBuf tmpBuf = allocator.allocateBuffer();
                    tmpBuf.writeBytes(b, bufferIndex * bufferSize, bufferSize);
                    return response.writeNonBlocking(tmpBuf, false);
                });
            }
            pooledBuffer = allocator.allocateBuffer();
            pooledBuffer.writeBytes(b, (bufferCount - 1) * bufferSize, remainder);
        }

        return ret.thenCompose(v -> asyncUpdateWritten(len));
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
