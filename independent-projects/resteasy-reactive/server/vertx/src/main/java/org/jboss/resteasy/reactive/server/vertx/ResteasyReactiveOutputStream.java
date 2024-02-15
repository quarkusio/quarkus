package org.jboss.resteasy.reactive.server.vertx;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedMap;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.server.core.LazyResponse;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.impl.HttpServerRequestInternal;

public class ResteasyReactiveOutputStream extends OutputStream {

    private static final Logger log = Logger.getLogger("org.jboss.resteasy.reactive.server.vertx.ResteasyReactiveOutputStream");
    private final ResteasyReactiveRequestContext context;
    protected final HttpServerRequest request;
    private final AppendBuffer appendBuffer;
    private boolean committed;

    private boolean closed;
    protected boolean waitingForDrain;
    protected boolean drainHandlerRegistered;
    protected boolean first = true;
    protected Throwable throwable;
    private ByteArrayOutputStream overflow;

    public ResteasyReactiveOutputStream(VertxResteasyReactiveRequestContext context) {
        this.context = context;
        this.request = context.getContext().request();
        this.appendBuffer = AppendBuffer.withMinChunks(PooledByteBufAllocator.DEFAULT,
                context.getDeployment().getResteasyReactiveConfig().getMinChunkSize(),
                context.getDeployment().getResteasyReactiveConfig().getOutputBufferSize());
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
        Handler<Void> handler = new DrainHandler(this);
        request.response().drainHandler(handler);
        request.response().closeHandler(handler);

        context.getContext().addEndHandler(new Handler<AsyncResult<Void>>() {
            @Override
            public void handle(AsyncResult<Void> event) {
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
            request.response().end((Handler<AsyncResult<Void>>) null);
            return;
        }
        //do all this in the same lock
        synchronized (request.connection()) {
            try {
                boolean bufferRequired = awaitWriteable() || (overflow != null && overflow.size() > 0);
                if (bufferRequired) {
                    //just buffer everything
                    //                    registerDrainHandler();
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
                        request.response().end(createBuffer(data), null);
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

    //    private void registerDrainHandler() {
    //        if (!drainHandlerRegistered) {
    //            drainHandlerRegistered = true;
    //            Handler<Void> handler = new DrainHandler(this);
    //            request.response().drainHandler(handler);
    //            request.response().closeHandler(handler);
    //        }
    //    }

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

    public void writeBlocking(ByteBuf buffer, boolean finished) throws IOException {
        prepareWrite(buffer, finished);
        write(buffer, finished);
    }

    private void prepareWrite(ByteBuf buffer, boolean finished) throws IOException {
        if (!committed) {
            committed = true;
            if (finished) {
                if (!context.serverResponse().headWritten()) {
                    if (buffer == null) {
                        context.serverResponse().setResponseHeader(HttpHeaderNames.CONTENT_LENGTH, "0");
                    } else {
                        context.serverResponse().setResponseHeader(HttpHeaderNames.CONTENT_LENGTH, "" + buffer.readableBytes());
                    }
                }
            } else {
                var contentLengthSet = contentLengthSet(request, context.getResponse());
                if (contentLengthSet == ContentLengthSetResult.NOT_SET) {
                    request.response().setChunked(true);
                } else if (contentLengthSet == ContentLengthSetResult.IN_JAX_RS_HEADER) {
                    // we need to make sure the content-length header is copied to Vert.x headers
                    // otherwise we could run into a race condition: see https://github.com/quarkusio/quarkus/issues/26599
                    Object contentLength = context.getResponse().get().getHeaders().getFirst(HttpHeaders.CONTENT_LENGTH);
                    context.serverResponse().setResponseHeader(HttpHeaderNames.CONTENT_LENGTH, contentLength.toString());
                }
            }
        }
    }

    public static ContentLengthSetResult contentLengthSet(HttpServerRequest request, LazyResponse lazyResponse) {
        if (request.response().headers().contains(HttpHeaderNames.CONTENT_LENGTH)) {
            return ContentLengthSetResult.IN_VERTX_HEADER;
        }
        if (!lazyResponse.isCreated()) {
            return ContentLengthSetResult.NOT_SET;
        }
        MultivaluedMap<String, Object> responseHeaders = lazyResponse.get().getHeaders();
        return (responseHeaders != null) && responseHeaders.containsKey(HttpHeaders.CONTENT_LENGTH)
                ? ContentLengthSetResult.IN_JAX_RS_HEADER
                : ContentLengthSetResult.NOT_SET;
    }

    public enum ContentLengthSetResult {
        NOT_SET,
        IN_VERTX_HEADER,
        IN_JAX_RS_HEADER
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
        private final ResteasyReactiveOutputStream out;

        public DrainHandler(ResteasyReactiveOutputStream out) {
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
