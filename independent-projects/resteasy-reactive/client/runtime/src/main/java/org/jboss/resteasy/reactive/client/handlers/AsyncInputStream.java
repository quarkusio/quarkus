package org.jboss.resteasy.reactive.client.handlers;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.ReadStream;
import io.vertx.core.streams.impl.InboundBuffer;

/**
 * Adapt an InputStream to a ReadStream that can be used with a Pump in Vertx.
 */
public class AsyncInputStream implements ReadStream<Buffer>, AutoCloseable {
    public static final String INPUTSTREAM_IS_CLOSED = "Inputstream is closed";
    private static int BUF_SIZE = 8192;
    // Based on the inputStream with the real data
    private final InputStream in;
    private final Context context;
    private final InboundBuffer<Buffer> queue;
    private final byte[] bytes = new byte[BUF_SIZE];
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final AtomicBoolean readInProgress = new AtomicBoolean(false);
    private Handler<Buffer> dataHandler;
    private Handler<Void> endHandler;
    private Handler<Throwable> exceptionHandler;

    /**
     * Create a new Async InputStream that can we used with a Pump
     */
    public AsyncInputStream(final Vertx vertx, final InputStream in) {
        this.context = vertx.getOrCreateContext();
        this.in = in;
        queue = new InboundBuffer<>(context, 0);
        queue.handler(buff -> {
            if (buff.length() > 0) {
                handleData(buff);
            } else {
                handleEnd();
            }
        });
        queue.drainHandler(v -> doRead());
    }

    @Override
    public void close() {
        closeInternal(null);
    }

    private synchronized void closeInternal(final Handler<AsyncResult<Void>> handler) {
        checkClose();
        closed.set(true);
        doClose(handler);
    }

    private void doClose(final Handler<AsyncResult<Void>> handler) {
        try {
            in.close();
        } catch (IOException ignored) {
            // Ignore
        }
        if (handler != null) {
            context.runOnContext(v -> handler.handle(Future.succeededFuture()));
        }
    }

    /**
     * Close using a specific handler.
     */
    public void close(final Handler<AsyncResult<Void>> handler) {
        closeInternal(handler);
    }

    @Override
    public synchronized AsyncInputStream exceptionHandler(final Handler<Throwable> exceptionHandler) {
        checkClose();
        this.exceptionHandler = exceptionHandler;
        return this;
    }

    private void checkClose() {
        if (closed.get()) {
            throw new IllegalStateException(INPUTSTREAM_IS_CLOSED);
        }
    }

    @Override
    public AsyncInputStream handler(final Handler<Buffer> handler) {
        checkClose();
        dataHandler = handler;
        if (dataHandler != null && !closed.get()) {
            doRead();
        } else {
            queue.clear();
        }
        return this;
    }

    @Override
    public AsyncInputStream pause() {
        checkClose();
        queue.pause();
        return this;
    }

    @Override
    public AsyncInputStream resume() {
        if (closed.get()) {
            return this;
        }
        queue.resume();
        return this;
    }

    @Override
    public ReadStream<Buffer> fetch(final long amount) {
        queue.fetch(amount);
        return this;
    }

    @Override
    public synchronized AsyncInputStream endHandler(final Handler<Void> endHandler) {
        checkClose();
        this.endHandler = endHandler;
        return this;
    }

    private void doRead() {
        checkClose();
        doRead(BUF_SIZE);
    }

    private void doRead(final int len) {
        if (readInProgress.compareAndSet(false, true)) {
            doRead(len, ar -> {
                if (ar.succeeded()) {
                    readInProgress.set(false);
                    final var buffer = ar.result();
                    // Empty buffer represents end of file
                    if (queue.write(buffer) && buffer.length() > 0) {
                        doRead(len);
                    }
                } else {
                    handleException(ar.cause());
                }
            });
        }
    }

    private void doRead(final int length, final Handler<AsyncResult<Buffer>> handler) {
        try {
            var bytesRead = 0;
            while (true) {
                bytesRead = in.read(bytes, 0, length);
                if (bytesRead == -1) {
                    //End of file
                    context.runOnContext(v -> handler.handle(Future.succeededFuture(Buffer.buffer(0))));
                    return;
                } else if (bytesRead > 0) {
                    // Lazy version
                    final var readed = bytesRead;
                    context.runOnContext(v -> {
                        final var buff = Buffer.buffer(readed);
                        buff.setBytes(0, bytes, 0, readed);
                        handler.handle(Future.succeededFuture(buff));
                    });
                    return;
                }
            }
        } catch (final Exception e) {
            context.runOnContext(v -> handler.handle(Future.failedFuture(e)));
        }
    }

    private void handleData(final Buffer buff) {
        if (dataHandler != null) {
            dataHandler.handle(buff);
        }
    }

    private void handleEnd() {
        dataHandler = null;
        if (endHandler != null) {
            endHandler.handle(null);
        }
    }

    private void handleException(final Throwable t) {
        if (exceptionHandler != null) {
            exceptionHandler.handle(t);
        }
    }

}
