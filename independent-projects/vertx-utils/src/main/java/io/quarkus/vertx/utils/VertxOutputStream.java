package io.quarkus.vertx.utils;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.jboss.logging.Logger;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponse;
import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;

/**
 * An {@link OutputStream} forwarding the bytes to Vert.x Web {@link HttpResponse}.
 * Suitable for porting frameworks such as RESTeasy or CXF to Vert.x.
 */
public class VertxOutputStream extends OutputStream {

    private static final Logger log = Logger.getLogger("org.jboss.resteasy.reactive.server.vertx.ResteasyReactiveOutputStream");

    private final VertxJavaIoContext context;
    private final HttpServerRequest request;
    private final AppendBuffer appendBuffer;
    private final HttpServerResponse response;
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition drainCondition = lock.newCondition();

    private boolean committed;
    private boolean closed;
    private boolean waitingForDrain;
    private Throwable throwable;

    public VertxOutputStream(VertxJavaIoContext context) {
        this.context = context;
        this.request = context.getRoutingContext().request();
        this.appendBuffer = AppendBuffer.withMinChunks(
                context.getMinChunkSize(),
                context.getOutputBufferCapacity());
        response = request.response();
        response.exceptionHandler(new Handler<Throwable>() {
            @Override
            public void handle(Throwable event) {
                throwable = event;
                log.debugf(event, "IO Exception ");
                request.connection().close();
                signalDrain();
            }
        });
        Handler<Void> drainHandler = new Handler<Void>() {
            @Override
            public void handle(Void event) {
                signalDrain();
            }
        };
        response.drainHandler(drainHandler);
        response.closeHandler(drainHandler);
        context.getRoutingContext().addEndHandler(new Handler<AsyncResult<Void>>() {
            @Override
            public void handle(AsyncResult<Void> event) {
                signalDrain();
            }
        });
    }

    private void signalDrain() {
        lock.lock();
        try {
            if (waitingForDrain) {
                drainCondition.signalAll();
            }
        } finally {
            lock.unlock();
        }
    }

    private Buffer createBuffer(ByteBuf data) {
        return new NoBoundChecksBuffer(data);
    }

    private void write(ByteBuf data, boolean last) throws IOException {
        if (last && data == null) {
            response.end();
            return;
        }
        lock.lock();
        try {
            awaitWriteable();
            if (last) {
                if (!response.ended()) {
                    response.end(createBuffer(data));
                }
            } else {
                response.write(createBuffer(data));
            }
        } catch (Exception e) {
            if (data != null && data.refCnt() > 0) {
                data.release();
            }
            throw new IOException("Failed to write", e);
        } finally {
            lock.unlock();
        }
    }

    private void awaitWriteable() throws IOException {
        // NEVER block the event loop!
        if (Context.isOnEventLoopThread()) {
            return;
        }
        assert lock.isHeldByCurrentThread();
        while (response.writeQueueFull()) {
            if (throwable != null) {
                throw new IOException(throwable);
            }
            if (response.closed()) {
                throw new IOException("Connection has been closed");
            }
            try {
                waitingForDrain = true;
                if (!drainCondition.await(1, TimeUnit.SECONDS)) {
                    if (request.response().ended() || request.response().closed()) {
                        if (throwable != null) {
                            throw new IOException(throwable);
                        } else {
                            throw new IOException("Connection has been closed");
                        }
                    }
                }
            } catch (InterruptedException e) {
                throw new InterruptedIOException(e.getMessage());
            } finally {
                waitingForDrain = false;
            }
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
}
