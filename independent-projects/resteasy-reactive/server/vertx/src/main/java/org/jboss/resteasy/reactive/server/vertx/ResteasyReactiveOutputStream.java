package org.jboss.resteasy.reactive.server.vertx;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;

public class ResteasyReactiveOutputStream extends OutputStream {

    private static final Logger log = Logger.getLogger("org.jboss.resteasy.reactive.server.vertx.ResteasyReactiveOutputStream");
    private final ResteasyReactiveRequestContext context;
    protected final HttpServerRequest request;
    private ByteBuf pooledBuffer;
    private long written;
    private boolean committed;

    private boolean closed;
    private boolean finished;
    protected boolean waitingForDrain;
    protected boolean drainHandlerRegistered;
    protected boolean first = true;
    protected Throwable throwable;
    private ByteArrayOutputStream overflow;

    public ResteasyReactiveOutputStream(VertxResteasyReactiveRequestContext context) {
        this.context = context;
        this.request = context.getContext().request();
        request.response().exceptionHandler(new Handler<Throwable>() {
            @Override
            public void handle(Throwable event) {
                throwable = event;
                log.debugf(event, "IO Exception ");
                //TODO: do we need this?
                terminateResponse();
                request.connection().close();
                synchronized (request.connection()) {
                    if (waitingForDrain) {
                        request.connection().notifyAll();
                    }
                }
            }
        });

        request.response().endHandler(new Handler<Void>() {
            @Override
            public void handle(Void event) {
                synchronized (request.connection()) {
                    if (waitingForDrain) {
                        request.connection().notifyAll();
                    }
                }
                terminateResponse();
            }
        });
    }

    public void terminateResponse() {

    }

    Buffer createBuffer(ByteBuf data) {
        return new VertxBufferImpl(data);
    }

    public void write(ByteBuf data, boolean last) throws IOException {
        if (last && data == null) {
            request.response().end();
            return;
        }
        //do all this in the same lock
        synchronized (request.connection()) {
            try {
                boolean bufferRequired = awaitWriteable() || (overflow != null && overflow.size() > 0);
                if (bufferRequired) {
                    //just buffer everything
                    registerDrainHandler();
                    if (overflow == null) {
                        overflow = new ByteArrayOutputStream();
                    }
                    if (data.hasArray()) {
                        overflow.write(data.array(), data.arrayOffset() + data.readerIndex(), data.readableBytes());
                    } else {
                        data.getBytes(data.readerIndex(), overflow, data.readableBytes());
                    }
                    if (last) {
                        closed = true;
                    }
                } else {
                    if (last) {
                        request.response().end(createBuffer(data));
                    } else {
                        request.response().write(createBuffer(data));
                    }
                }
            } catch (Exception e) {
                if (data != null && data.refCnt() > 0) {
                    data.release();
                }
                throw new IOException("Failed to write", e);
            }
        }
    }

    private boolean awaitWriteable() throws IOException {
        if (Context.isOnEventLoopThread()) {
            return request.response().writeQueueFull();
        }
        if (first) {
            first = false;
            return false;
        }
        assert Thread.holdsLock(request.connection());
        while (request.response().writeQueueFull()) {
            if (throwable != null) {
                throw new IOException(throwable);
            }
            if (request.response().closed()) {
                throw new IOException("Connection has been closed");
            }
            registerDrainHandler();
            try {
                waitingForDrain = true;
                request.connection().wait();
            } catch (InterruptedException e) {
                throw new InterruptedIOException(e.getMessage());
            } finally {
                waitingForDrain = false;
            }
        }
        return false;
    }

    private void registerDrainHandler() {
        if (!drainHandlerRegistered) {
            drainHandlerRegistered = true;
            Handler<Void> handler = new Handler<Void>() {
                @Override
                public void handle(Void event) {
                    synchronized (request.connection()) {
                        if (waitingForDrain) {
                            request.connection().notifyAll();
                        }
                        if (overflow != null) {
                            if (overflow.size() > 0) {
                                if (closed) {
                                    request.response().end(Buffer.buffer(overflow.toByteArray()));
                                } else {
                                    request.response().write(Buffer.buffer(overflow.toByteArray()));
                                }
                                overflow.reset();
                            }
                        }
                    }
                }
            };
            request.response().drainHandler(handler);
            request.response().closeHandler(handler);
        }
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
                pooledBuffer = buffer = PooledByteBufAllocator.DEFAULT.directBuffer();
            }
            while (rem > 0) {
                int toWrite = Math.min(rem, buffer.writableBytes());
                buffer.writeBytes(b, idx, toWrite);
                rem -= toWrite;
                idx += toWrite;
                if (!buffer.isWritable()) {
                    ByteBuf tmpBuf = buffer;
                    this.pooledBuffer = buffer = PooledByteBufAllocator.DEFAULT.directBuffer();
                    writeBlocking(tmpBuf, false);
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

    public void writeBlocking(ByteBuf buffer, boolean finished) throws IOException {
        prepareWrite(buffer, finished);
        write(buffer, finished);
    }

    private void prepareWrite(ByteBuf buffer, boolean finished) throws IOException {
        if (!committed) {
            committed = true;
            if (finished) {
                if (buffer == null) {
                    context.serverResponse().setResponseHeader(HttpHeaderNames.CONTENT_LENGTH, "0");
                } else {
                    context.serverResponse().setResponseHeader(HttpHeaderNames.CONTENT_LENGTH, "" + buffer.readableBytes());
                }
            } else if (!request.response().headers().contains(HttpHeaderNames.CONTENT_LENGTH)) {
                request.response().setChunked(true);
            }
        }
        if (finished) {
            this.finished = true;
        }
    }

    void updateWritten(final long len) throws IOException {
        this.written += len;
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
                writeBlocking(pooledBuffer, false);
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
            writeBlocking(pooledBuffer, true);
        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            closed = true;
            pooledBuffer = null;
        }
    }

}
