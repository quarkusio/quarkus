package io.quarkus.resteasy.runtime.standalone;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.util.ArrayDeque;
import java.util.Deque;

import io.netty.buffer.ByteBuf;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;

public class VertxInputStream extends InputStream {

    private final VertxBlockingInput exchange;

    private boolean closed;
    private boolean finished;
    private ByteBuf pooled;

    public VertxInputStream(HttpServerRequest request) {

        this.exchange = new VertxBlockingInput(request);
    }

    @Override
    public int read() throws IOException {
        byte[] b = new byte[1];
        int read = read(b);
        if (read == -1) {
            return -1;
        }
        return b[0] & 0xff;
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
                pooled = null;
            }
        }
    }

    @Override
    public int available() throws IOException {
        if (closed) {
            throw new IOException("Stream is closed");
        }
        if (finished) {
            return -1;
        }
        return exchange.readBytesAvailable();
    }

    @Override
    public void close() throws IOException {
        if (closed) {
            throw new IOException("Stream is closed");
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

    public class VertxBlockingInput implements Handler<Buffer> {
        protected final HttpServerRequest request;
        protected Buffer input1;
        protected Deque<Buffer> inputOverflow;
        protected boolean waiting = false;
        protected boolean eof = false;

        public VertxBlockingInput(HttpServerRequest request) {
            this.request = request;
            if (!request.isEnded()) {
                request.handler(this);
                request.endHandler(new Handler<Void>() {
                    @Override
                    public void handle(Void event) {
                        synchronized (request.connection()) {
                            eof = true;
                            if (waiting) {
                                request.connection().notify();
                            }
                        }
                        if (input1 == null) {
                            terminateRequest();
                        }

                    }
                });
                request.fetch(1);
            } else {
                terminateRequest();
            }
        }

        public void terminateRequest() {

        }

        protected ByteBuf readBlocking() throws IOException {
            synchronized (request.connection()) {
                while (input1 == null && !eof) {
                    try {
                        if (Context.isOnEventLoopThread()) {
                            throw new IOException("Attempting a blocking read on io thread");
                        }
                        waiting = true;
                        request.connection().wait();
                    } catch (InterruptedException e) {
                        throw new InterruptedIOException(e.getMessage());
                    } finally {
                        waiting = false;
                    }
                }
                Buffer ret = input1;
                input1 = null;
                if (inputOverflow != null) {
                    input1 = inputOverflow.poll();
                    if (input1 == null) {
                        request.fetch(1);
                    }
                } else {
                    request.fetch(1);
                }

                if (ret == null) {
                    terminateRequest();
                }
                return ret == null ? null : ret.getByteBuf();
            }
        }

        @Override
        public void handle(Buffer event) {
            synchronized (request.connection()) {
                if (input1 == null) {
                    input1 = event;
                } else {
                    if (inputOverflow == null) {
                        inputOverflow = new ArrayDeque<>();
                    }
                    inputOverflow.add(event);
                }
                if (waiting) {
                    request.connection().notifyAll();
                }
            }
        }

        public int readBytesAvailable() {
            if (input1 != null) {
                return input1.getByteBuf().readableBytes();
            }
            return 0;
        }
    }

}
