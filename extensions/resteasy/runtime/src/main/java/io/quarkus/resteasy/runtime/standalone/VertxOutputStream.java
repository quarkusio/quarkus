package io.quarkus.resteasy.runtime.standalone;

import java.io.IOException;
import java.io.OutputStream;

import javax.ws.rs.core.HttpHeaders;

import io.netty.buffer.ByteBuf;

public class VertxOutputStream extends OutputStream {

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

}
