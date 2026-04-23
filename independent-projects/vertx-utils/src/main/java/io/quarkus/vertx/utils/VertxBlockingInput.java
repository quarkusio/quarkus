package io.quarkus.vertx.utils;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import io.netty.buffer.ByteBuf;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.ReadStream;

/**
 * Shared blocking-read logic for Vert.x {@link ReadStream} instances (both server and client).
 * Uses {@link ReentrantLock}/{@link Condition} instead of {@code synchronized}/{@code wait}/{@code notifyAll}
 * to avoid virtual-thread pinning on JDK 21+.
 */
public class VertxBlockingInput implements Handler<Buffer> {

    private final ReadStream<Buffer> stream;
    private final long timeout;
    private final Runnable onTimeout;
    private final boolean handleHttp2EmptyBufferAsEof;
    private final Supplier<? extends RuntimeException> blockingExceptionSupplier;

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition hasData = lock.newCondition();

    private Buffer input1;
    private Deque<Buffer> inputOverflow;
    private boolean waiting;
    private boolean eof;
    private Throwable readException;

    /**
     * Creates a new blocking input that sets up data/end/exception handlers on the given stream.
     * If the stream has already ended, the constructor catches {@link IllegalStateException}
     * and marks this input as EOF.
     *
     * @param stream the read stream to consume
     * @param timeout read timeout in milliseconds
     * @param onTimeout action to run when a read times out (e.g. close connection or socket)
     * @param handleHttp2EmptyBufferAsEof if {@code true}, a zero-length buffer is treated as EOF (needed for HTTP/2 server)
     * @param blockingExceptionSupplier supplier for the exception thrown when a blocking read is attempted on the event loop
     * @param initialError if non-null, the stream is immediately marked as failed with this error
     */
    public VertxBlockingInput(ReadStream<Buffer> stream, long timeout,
            Runnable onTimeout, boolean handleHttp2EmptyBufferAsEof,
            Supplier<? extends RuntimeException> blockingExceptionSupplier,
            Throwable initialError) {
        this.stream = stream;
        this.timeout = timeout;
        this.onTimeout = onTimeout;
        this.handleHttp2EmptyBufferAsEof = handleHttp2EmptyBufferAsEof;
        this.blockingExceptionSupplier = blockingExceptionSupplier;

        lock.lock();
        try {
            if (initialError != null) {
                readException = initialError;
                return;
            }
            try {
                stream.pause();
                stream.handler(this);
                stream.endHandler(new Handler<Void>() {
                    @Override
                    public void handle(Void event) {
                        lock.lock();
                        try {
                            eof = true;
                            if (waiting) {
                                hasData.signalAll();
                            }
                        } finally {
                            lock.unlock();
                        }
                    }
                });
                stream.exceptionHandler(new Handler<Throwable>() {
                    @Override
                    public void handle(Throwable event) {
                        lock.lock();
                        try {
                            readException = new IOException(event);
                            if (input1 != null) {
                                input1.getByteBuf().release();
                                input1 = null;
                            }
                            if (inputOverflow != null) {
                                Buffer d = inputOverflow.poll();
                                while (d != null) {
                                    d.getByteBuf().release();
                                    d = inputOverflow.poll();
                                }
                            }
                            if (waiting) {
                                hasData.signalAll();
                            }
                        } finally {
                            lock.unlock();
                        }
                    }
                });
                stream.fetch(1);
            } catch (IllegalStateException e) {
                // Stream already ended (happens on client side)
                eof = true;
            }
        } finally {
            lock.unlock();
        }
    }

    public ByteBuf readBlocking() throws IOException {
        long expire = System.currentTimeMillis() + timeout;
        lock.lock();
        try {
            while (input1 == null && !eof && readException == null) {
                long rem = expire - System.currentTimeMillis();
                if (rem <= 0) {
                    //everything is broken, if read has timed out we can assume that the underlying connection
                    //is wrecked, so just close it
                    onTimeout.run();
                    IOException throwable = new IOException("Read timed out");
                    readException = throwable;
                    throw throwable;
                }

                if (Context.isOnEventLoopThread()) {
                    throw blockingExceptionSupplier.get();
                }
                try {
                    waiting = true;
                    hasData.await(rem, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    throw new InterruptedIOException(e.getMessage());
                } finally {
                    waiting = false;
                }
            }
            if (readException != null) {
                throw new IOException(readException);
            }
            Buffer ret = input1;
            input1 = null;
            if (inputOverflow != null) {
                input1 = inputOverflow.poll();
                if (input1 == null) {
                    stream.fetch(1);
                }
            } else if (!eof) {
                stream.fetch(1);
            }
            return ret == null ? null : ret.getByteBuf();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void handle(Buffer event) {
        lock.lock();
        try {
            if (handleHttp2EmptyBufferAsEof && event.length() == 0) {
                // When using HTTP/2 H2, this indicates that we won't receive anymore data.
                eof = true;
                if (waiting) {
                    hasData.signalAll();
                }
                return;
            }
            if (input1 == null) {
                input1 = event;
            } else {
                if (inputOverflow == null) {
                    inputOverflow = new ArrayDeque<>();
                }
                inputOverflow.add(event);
            }
            if (waiting) {
                hasData.signalAll();
            }
        } finally {
            lock.unlock();
        }
    }

    public int readBytesAvailable() {
        lock.lock();
        try {
            if (input1 != null) {
                return input1.getByteBuf().readableBytes();
            }
            return 0;
        } finally {
            lock.unlock();
        }
    }

    public void discard() {
        stream.pause().handler(null).exceptionHandler(null).endHandler(null).resume();
    }
}
