package org.jboss.resteasy.reactive.client.handlers;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.jboss.resteasy.reactive.client.impl.RestClientRequestContext;
import org.jboss.resteasy.reactive.common.core.BlockingNotAllowedException;

import io.netty.buffer.ByteBuf;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpHeaders;

class VertxClientInputStream extends InputStream {
    public static final String MAX_REQUEST_SIZE_KEY = "io.quarkus.max-request-size";
    private final VertxBlockingInput exchange;
    private boolean closed;
    private boolean finished;
    private ByteBuf pooled;
    private final byte[] oneByte = new byte[1];
    private final RestClientRequestContext vertxResteasyReactiveRequestContext;

    public VertxClientInputStream(HttpClientResponse response, long timeout,
                                  RestClientRequestContext vertxResteasyReactiveRequestContext) {
        this.vertxResteasyReactiveRequestContext = vertxResteasyReactiveRequestContext;
        this.exchange = new VertxBlockingInput(response, timeout);
    }

    public VertxClientInputStream(HttpClientResponse request, long timeout, ByteBuf existing,
                                  RestClientRequestContext vertxResteasyReactiveRequestContext) {
        this.vertxResteasyReactiveRequestContext = vertxResteasyReactiveRequestContext;
        this.exchange = new VertxBlockingInput(request, timeout);
        this.pooled = existing;
    }

    @Override
    public int read() throws IOException {
        int read = read(oneByte);
        if (read == -1) {
            return -1;
        }
        return oneByte[0] & 0xff;
    }

    @Override
    public int read(final byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        if (closed) {
            throw new IOException("Stream is closed");
        }
        if (b == null || b.length < off + len) {
            throw new IOException("Not able to read into buffer");
        }
        readIntoBuffer();
        if (finished) {
            return -1;
        }
        if (len == 0) {
            return 0;
        }
        ByteBuf buffer = pooled;
        int copied = Math.min(len, buffer.readableBytes());
        buffer.readBytes(b, off, copied);
        if (!buffer.isReadable()) {
            pooled.release();
            pooled = null;
        }
        return copied;
    }

    private void readIntoBuffer() throws IOException {
        if (pooled == null && !finished) {
            pooled = exchange.readBlocking();
            if (pooled == null) {
                finished = true;
            }
        }
    }

    @Override
    public int available() throws IOException {
        if (closed) {
            throw new IOException("Stream is closed");
        }
        if (finished) {
            return 0;
        }
        if (pooled != null && pooled.isReadable()) {
            return pooled.readableBytes() + exchange.readBytesAvailable();
        }

        return exchange.readBytesAvailable();
    }

    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;
        try {
            while (!finished) {
                readIntoBuffer();
                if (pooled != null) {
                    pooled.release();
                    pooled = null;
                }
            }
        } catch (IOException | RuntimeException e) {
            //our exchange is all broken, just end it
            throw e;
        } finally {
            if (pooled != null) {
                pooled.release();
                pooled = null;
            }
            finished = true;
        }
    }

    public static class VertxBlockingInput implements Handler<Buffer> {
        protected final HttpClientResponse request;
        protected final Deque<Buffer> inputOverflow = new ConcurrentLinkedDeque<>();
        private static final int INTERNAL_READ_WAIT_MS = 50;
        protected boolean endOfWrite = false;
        protected IOException readException;
        private final long timeout;
        private final int headerLen;

        public VertxBlockingInput(HttpClientResponse response, long timeout) {
            this.request = response;
            this.headerLen = getLengthFromHeader();
            this.timeout = timeout;
            response.pause();
            response.handler(this);
            try {
                response.endHandler(new Handler<Void>() {
                    @Override
                    public void handle(Void event) {
                        endOfWrite = true;
                        wakeupReader();
                    }
                });
                response.exceptionHandler(new Handler<Throwable>() {
                    @Override
                    public void handle(Throwable event) {
                        readException = new IOException(event);
                        Buffer d = inputOverflow.poll();
                        while (d != null) {
                            d.getByteBuf().release();
                            d = inputOverflow.poll();
                        }
                        wakeupReader();
                    }
                });
                response.fetch(10);
            } catch (IllegalStateException e) {
                //already ended
                endOfWrite = true;
            }
        }

        private void wakeupReader() {
            synchronized (VertxBlockingInput.this) {
                VertxBlockingInput.this.notifyAll();
            }
        }

        private Buffer removeHead() throws IOException {
            if (inputOverflow.isEmpty()) {
                synchronized (VertxBlockingInput.this) {
                    try {
                        VertxBlockingInput.this.wait(INTERNAL_READ_WAIT_MS);
                    } catch (InterruptedException e) {
                        throw new InterruptedIOException(e.getMessage());
                    }
                }
            }
            return inputOverflow.poll();
        }

        protected ByteBuf readBlocking() throws IOException {
            long expire = System.currentTimeMillis() + timeout;
            Buffer ret;
            boolean status;
            if (readException != null) {
                throw readException;
            }
            // Preemptive fetch
            if (inputOverflow.isEmpty()) {
                request.fetch(1);
            }
            // First read before testing endOfWrite
            ret = removeHead();
            status = ret == null && !endOfWrite && readException == null;
            while (status) {
                long rem = expire - System.currentTimeMillis();
                if (rem <= 0) {
                    //everything is broken, if read has timed out we can assume that the underling connection
                    //is wrecked, so just close it
                    request.netSocket().close();
                    readException = new IOException("Read timed out");
                    throw readException;
                }

                if (Context.isOnEventLoopThread()) {
                    throw new BlockingNotAllowedException("Attempting a blocking read on io thread");
                }
                ret = removeHead();
                status = ret == null && !endOfWrite && readException == null;
                Thread.yield();
            }
            if (readException != null) {
                throw readException;
            }
            if (!endOfWrite && (inputOverflow.isEmpty())) {
                request.fetch(1);
            }
            if (ret == null && !inputOverflow.isEmpty()) {
                // Might not be the end yet if queue is not empty
                ret = inputOverflow.poll();
            }
            return ret == null ? null : ret.getByteBuf();
        }

        @Override
        public void handle(Buffer event) {
            if (readException == null) {
                inputOverflow.addLast(event);
            } else {
                event.getByteBuf().release();
            }
            wakeupReader();
        }

        public int readBytesAvailable() {
            Buffer buf = inputOverflow.peek();
            if (buf != null) {
                int len = buf.getByteBuf().readableBytes();
                if (len > 0) {
                    return len;
                }
            }
            return headerLen;
        }

        private int getLengthFromHeader() {
            String length = request.getHeader(HttpHeaders.CONTENT_LENGTH);
            if (length == null) {
                return 8192;
            }
            try {
                return Integer.parseInt(length);
            } catch (NumberFormatException e) {
                Long.parseLong(length); // ignore the value as can only return an int anyway
                return Integer.MAX_VALUE;
            }
        }
    }

}
