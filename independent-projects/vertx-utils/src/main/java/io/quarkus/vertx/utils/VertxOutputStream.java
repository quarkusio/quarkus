package io.quarkus.vertx.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.util.Optional;

import org.jboss.logging.Logger;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponse;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.impl.HttpServerRequestInternal;

/**
 * An {@link OutputStream} forwarding the bytes to Vert.x Web {@link HttpResponse}.
 * Suitable for porting frameworks such as RESTeasy or CXF to Vert.x.
 */
public class VertxOutputStream extends OutputStream {

    private static final Logger log = Logger.getLogger("org.jboss.resteasy.reactive.server.vertx.ResteasyReactiveOutputStream");

    private final VertxJavaIoContext context;
    private final HttpServerRequest request;
    private final AppendBuffer appendBuffer;

    private boolean committed;
    private boolean closed;
    private boolean waitingForDrain;
    private boolean first = true;
    private Throwable throwable;
    private ByteArrayOutputStream overflow;

    public VertxOutputStream(VertxJavaIoContext context) {
        this.context = context;
        this.request = context.getRoutingContext().request();
        this.appendBuffer = AppendBuffer.withMinChunks(
                context.getMinChunkSize(),
                context.getOutputBufferCapacity());
        request.response().exceptionHandler(new Handler<Throwable>() {
            @Override
            public void handle(Throwable event) {
                throwable = event;
                log.debugf(event, "IO Exception ");
                request.connection().close();
                synchronized (request.connection()) {
                    if (waitingForDrain) {
                        request.connection().notifyAll();
                    }
                }
            }
        });
        Handler<Void> handler = new DrainHandler(this);
        request.response().drainHandler(handler);
        request.response().closeHandler(handler);

        context.getRoutingContext().addEndHandler(new Handler<AsyncResult<Void>>() {
            @Override
            public void handle(AsyncResult<Void> event) {
                synchronized (request.connection()) {
                    if (waitingForDrain) {
                        request.connection().notifyAll();
                    }
                }
            }
        });
    }

    private Buffer createBuffer(ByteBuf data) {
        return new NoBoundChecksBuffer(data);
    }

    private void write(ByteBuf data, boolean last) throws IOException {
        if (last && data == null) {
            request.response().end((Handler<AsyncResult<Void>>) null);
            return;
        }
        //do all this in the same lock
        synchronized (request.connection()) {
            try {
                boolean bufferRequired = awaitWriteable() || (overflow != null && overflow.size() > 0);
                if (bufferRequired) {
                    //just buffer everything
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
                    data.release();
                } else {
                    if (last) {
                        if (!request.response().ended()) { // can happen when an exception occurs during JSON serialization with Jackson
                            request.response().end(createBuffer(data), null);
                        }
                    } else {
                        request.response().write(createBuffer(data), null);
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
        if (Vertx.currentContext() == ((HttpServerRequestInternal) request).context()) {
            return false; // we are on the (right) event loop, so we can write - Netty will do the right thing.
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
            //            registerDrainHandler();
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
        try {
            while (rem > 0) {
                final int written = appendBuffer.append(b, idx, rem);
                if (written < rem) {
                    writeBlocking(appendBuffer.clear(), false);
                }
                rem -= written;
                idx += written;
            }
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    private void writeBlocking(ByteBuf buffer, boolean finished) throws IOException {
        prepareWrite(buffer, finished);
        write(buffer, finished);
    }

    private void prepareWrite(ByteBuf buffer, boolean finished) throws IOException {
        if (!committed) {
            committed = true;
            final HttpServerResponse response = request.response();
            if (finished) {
                if (!response.headWritten()) {
                    if (buffer == null) {
                        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, "0");
                    } else {
                        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, String.valueOf(buffer.readableBytes()));
                    }
                }
            } else {
                final Optional<String> contentLength = context.getContentLength();
                if (contentLength.isEmpty()) {
                    response.setChunked(true);
                } else {
                    /* Pass the content length value from the framework writing into this stream */
                    response.headers().set(HttpHeaderNames.CONTENT_LENGTH, contentLength.get());
                }
            }
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
            var toFlush = appendBuffer.clear();
            if (toFlush != null) {
                writeBlocking(toFlush, false);
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
            writeBlocking(appendBuffer.clear(), true);
        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            closed = true;
        }
    }

    private static class DrainHandler implements Handler<Void> {
        private final VertxOutputStream out;

        public DrainHandler(VertxOutputStream out) {
            this.out = out;
        }

        @Override
        public void handle(Void event) {
            synchronized (out.request.connection()) {
                if (out.waitingForDrain) {
                    out.request.connection().notifyAll();
                }
                if (out.overflow != null) {
                    if (out.overflow.size() > 0) {
                        if (out.closed) {
                            out.request.response().end(Buffer.buffer(out.overflow.toByteArray()), null);
                        } else {
                            out.request.response().write(Buffer.buffer(out.overflow.toByteArray()), null);
                        }
                        out.overflow.reset();
                    }
                }
            }
        }
    }
}
