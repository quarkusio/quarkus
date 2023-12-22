package org.jboss.resteasy.reactive.client.handlers;

import java.io.IOException;
import java.io.InputStream;
import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.impl.ContextInternal;
import io.vertx.core.streams.ReadStream;

/**
 * Adapt an InputStream to a ReadStream that can be used with a Pump in Vertx.
 */
public class AsyncInputStream implements ReadStream<Buffer>, AutoCloseable {
    public static final String INPUTSTREAM_IS_CLOSED = "Inputstream is closed";
    // Based on the inputStream with the real data
    private final InputStream in;
    private final Context context;
    private final InboundBuffer queue;
    private final byte[] bytes;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final AtomicBoolean readInProgress = new AtomicBoolean(false);
    private Handler<Buffer> dataHandler;
    private Handler<Void> endHandler;
    private Handler<Throwable> exceptionHandler;
    private final int maxChunkSize;

    /**
     * Create a new Async InputStream that can we used with a Pump
     */
    public AsyncInputStream(final Vertx vertx, final InputStream in, final int maxChunkSize) {
        this.maxChunkSize = Math.max(maxChunkSize, 8192);
        bytes = new byte[this.maxChunkSize];
        this.context = vertx.getOrCreateContext();
        this.in = in;
        queue = new InboundBuffer(context);
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
        doRead(maxChunkSize);
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

    /**
     * To be not dependent from InboundBuffer Vertx implementation
     */
    public static class InboundBuffer {
        private final ContextInternal context;
        private final Deque<Buffer> pending = new LinkedList<>();
        private final long highWaterMark;
        private long demand;
        private Handler<Buffer> handler;
        private boolean overflow;
        private Handler<Void> drainHandler;
        private Handler<Void> emptyHandler;
        private Handler<Throwable> exceptionHandler;
        private boolean emitting;

        public InboundBuffer(Context context) {
            this(context, 16L);
        }

        public InboundBuffer(Context context, long highWaterMark) {
            if (context == null) {
                throw new NullPointerException("context must not be null");
            }
            if (highWaterMark < 0) {
                throw new IllegalArgumentException("highWaterMark " + highWaterMark + " >= 0");
            }
            this.context = (ContextInternal) context;
            this.highWaterMark = highWaterMark;
            this.demand = Long.MAX_VALUE;
        }

        private void checkThread() {
            if (!context.inThread()) {
                throw new IllegalStateException("This operation must be called from a Vert.x thread");
            }
        }

        /**
         * Write an {@code element} to the buffer. The element will be delivered synchronously to the handler when
         * it is possible, otherwise it will be queued for later delivery.
         *
         * @param element the element to add
         * @return {@code false} when the producer should stop writing
         */
        public boolean write(Buffer element) {
            checkThread();
            Handler<Buffer> handlerTmp;
            synchronized (this) {
                if (demand == 0L || emitting) {
                    pending.add(element);
                    return checkWritable();
                } else {
                    if (demand != Long.MAX_VALUE) {
                        --demand;
                    }
                    emitting = true;
                    handlerTmp = this.handler;
                }
            }
            handleEvent(handlerTmp, element);
            return emitPending();
        }

        private boolean checkWritable() {
            if (demand == Long.MAX_VALUE) {
                return true;
            } else {
                long actual = size() - demand;
                boolean writable = actual < highWaterMark;
                overflow |= !writable;
                return writable;
            }
        }

        /**
         * Write an {@code iterable} of {@code elements}.
         *
         * @param elements the elements to add
         * @return {@code false} when the producer should stop writing
         * @see #write(Buffer)
         */
        public boolean write(Iterable<Buffer> elements) {
            checkThread();
            synchronized (this) {
                for (Buffer element : elements) {
                    pending.add(element);
                }
                if (demand == 0L || emitting) {
                    return checkWritable();
                } else {
                    emitting = true;
                }
            }
            return emitPending();
        }

        private boolean emitPending() {
            Buffer element;
            Handler<Buffer> h;
            while (true) {
                synchronized (this) {
                    int size = size();
                    if (demand == 0L) {
                        emitting = false;
                        boolean writable = size < highWaterMark;
                        overflow |= !writable;
                        return writable;
                    } else if (size == 0) {
                        emitting = false;
                        return true;
                    }
                    if (demand != Long.MAX_VALUE) {
                        demand--;
                    }
                    element = pending.poll();
                    h = this.handler;
                }
                handleEvent(h, element);
            }
        }

        /**
         * Drain the buffer.
         * <p/>
         * Calling this assumes {@code (demand > 0L && !pending.isEmpty()) == true}
         */
        private void drain() {
            int emitted = 0;
            Handler<Void> drainHandlerTmp;
            Handler<Void> emptyHandlerTmp;
            while (true) {
                Buffer element;
                Handler<Buffer> handlerTmp;
                synchronized (this) {
                    int size = size();
                    if (size == 0) {
                        emitting = false;
                        if (overflow) {
                            overflow = false;
                            drainHandlerTmp = this.drainHandler;
                        } else {
                            drainHandlerTmp = null;
                        }
                        emptyHandlerTmp = emitted > 0 ? this.emptyHandler : null;
                        break;
                    } else if (demand == 0L) {
                        emitting = false;
                        return;
                    }
                    emitted++;
                    if (demand != Long.MAX_VALUE) {
                        demand--;
                    }
                    element = pending.poll();
                    handlerTmp = this.handler;
                }
                handleEvent(handlerTmp, element);
            }
            if (drainHandlerTmp != null) {
                handleEvent(drainHandlerTmp, null);
            }
            if (emptyHandlerTmp != null) {
                handleEvent(emptyHandlerTmp, null);
            }
        }

        private <T> void handleEvent(Handler<T> handler, T element) {
            if (handler != null) {
                try {
                    handler.handle(element);
                } catch (Throwable t) {
                    handleException(t);
                }
            }
        }

        private void handleException(Throwable err) {
            Handler<Throwable> handlerTmp;
            synchronized (this) {
                if ((handlerTmp = exceptionHandler) == null) {
                    return;
                }
            }
            handlerTmp.handle(err);
        }

        /**
         * Request a specific {@code amount} of elements to be fetched, the amount is added to the actual demand.
         * <p/>
         * Pending elements in the buffer will be delivered asynchronously on the context to the handler.
         * <p/>
         * This method can be called from any thread.
         *
         * @return {@code true} when the buffer will be drained
         */
        public boolean fetch(long amount) {
            if (amount < 0L) {
                throw new IllegalArgumentException();
            }
            synchronized (this) {
                demand += amount;
                if (demand < 0L) {
                    demand = Long.MAX_VALUE;
                }
                if (emitting || (isEmpty() && !overflow)) {
                    return false;
                }
                emitting = true;
            }
            context.runOnContext(v -> drain());
            return true;
        }

        /**
         * Read the most recent element synchronously.
         * <p/>
         * No handler will be called.
         *
         * @return the most recent element or {@code null} if no element was in the buffer
         */
        public Buffer read() {
            synchronized (this) {
                if (isEmpty()) {
                    return null;
                }
                return pending.poll();
            }
        }

        /**
         * Clear the buffer synchronously.
         * <p/>
         * No handler will be called.
         *
         * @return a reference to this, so the API can be used fluently
         */
        public synchronized InboundBuffer clear() {
            if (isEmpty()) {
                return this;
            }
            pending.clear();
            return this;
        }

        /**
         * Pause the buffer, it sets the buffer in {@code fetch} mode and clears the actual demand.
         *
         * @return a reference to this, so the API can be used fluently
         */
        public synchronized InboundBuffer pause() {
            demand = 0L;
            return this;
        }

        /**
         * Resume the buffer, and sets the buffer in {@code flowing} mode.
         * <p/>
         * Pending elements in the buffer will be delivered asynchronously on the context to the handler.
         * <p/>
         * This method can be called from any thread.
         *
         * @return {@code true} when the buffer will be drained
         */
        public boolean resume() {
            return fetch(Long.MAX_VALUE);
        }

        /**
         * Set an {@code handler} to be called with elements available from this buffer.
         *
         * @param handler the handler
         * @return a reference to this, so the API can be used fluently
         */
        public synchronized InboundBuffer handler(Handler<Buffer> handler) {
            this.handler = handler;
            return this;
        }

        /**
         * Set an {@code handler} to be called when the buffer is drained and the producer can resume writing to the buffer.
         *
         * @param handler the handler to be called
         * @return a reference to this, so the API can be used fluently
         */
        public synchronized InboundBuffer drainHandler(Handler<Void> handler) {
            drainHandler = handler;
            return this;
        }

        /**
         * Set an {@code handler} to be called when the buffer becomes empty.
         *
         * @param handler the handler to be called
         * @return a reference to this, so the API can be used fluently
         */
        public synchronized InboundBuffer emptyHandler(Handler<Void> handler) {
            emptyHandler = handler;
            return this;
        }

        /**
         * Set an {@code handler} to be called when an exception is thrown by an handler.
         *
         * @param handler the handler
         * @return a reference to this, so the API can be used fluently
         */
        public synchronized InboundBuffer exceptionHandler(Handler<Throwable> handler) {
            exceptionHandler = handler;
            return this;
        }

        /**
         * @return whether the buffer is empty
         */
        public synchronized boolean isEmpty() {
            return pending.isEmpty();
        }

        /**
         * @return whether the buffer is writable
         */
        public synchronized boolean isWritable() {
            return size() < highWaterMark;
        }

        /**
         * @return whether the buffer is paused, i.e it is in {@code fetch} mode and the demand is {@code 0}.
         */
        public synchronized boolean isPaused() {
            return demand == 0L;
        }

        /**
         * @return the actual number of elements in the buffer
         */
        public synchronized int size() {
            return pending.size();
        }
    }
}
