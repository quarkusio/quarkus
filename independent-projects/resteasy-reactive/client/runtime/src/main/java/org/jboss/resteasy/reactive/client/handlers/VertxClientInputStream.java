package org.jboss.resteasy.reactive.client.handlers;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.util.ArrayDeque;
import java.util.Deque;

import org.jboss.resteasy.reactive.common.core.BlockingNotAllowedException;

import io.netty.buffer.ByteBuf;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpHeaders;

class VertxClientInputStream extends InputStream {

    private final VertxBlockingInput exchange;

    private boolean closed;
    private boolean finished;
    private ByteBuf pooled;

    public VertxClientInputStream(HttpClientResponse response, long timeout) {
        this.exchange = new VertxBlockingInput(response, timeout);
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
            return 0;
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
        protected Buffer input1;
        protected Deque<Buffer> inputOverflow;
        protected boolean waiting = false;
        protected boolean eof = false;
        protected Throwable readException;
        private final long timeout;

        public VertxBlockingInput(HttpClientResponse response, long timeout) {
            this.request = response;
            this.timeout = timeout;
            response.pause();
            response.handler(this);
            try {
                response.endHandler(new Handler<Void>() {
                    @Override
                    public void handle(Void event) {
                        synchronized (VertxBlockingInput.this) {
                            eof = true;
                            if (waiting) {
                                VertxBlockingInput.this.notify();
                            }
                        }
                    }
                });
                response.exceptionHandler(new Handler<Throwable>() {
                    @Override
                    public void handle(Throwable event) {
                        synchronized (VertxBlockingInput.this) {
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
                                VertxBlockingInput.this.notify();
                            }
                        }
                    }

                });
                response.fetch(1);
            } catch (IllegalStateException e) {
                //already ended
                eof = true;
            }
        }

        protected ByteBuf readBlocking() throws IOException {
            long expire = System.currentTimeMillis() + timeout;
            synchronized (VertxBlockingInput.this) {
                while (input1 == null && !eof && readException == null) {
                    long rem = expire - System.currentTimeMillis();
                    if (rem <= 0) {
                        //everything is broken, if read has timed out we can assume that the underling connection
                        //is wrecked, so just close it
                        request.netSocket().close();
                        IOException throwable = new IOException("Read timed out");
                        readException = throwable;
                        throw throwable;
                    }

                    try {
                        if (Context.isOnEventLoopThread()) {
                            throw new BlockingNotAllowedException("Attempting a blocking read on io thread");
                        }
                        waiting = true;
                        VertxBlockingInput.this.wait(rem);
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
                        request.fetch(1);
                    }
                } else if (!eof) {
                    request.fetch(1);
                }
                return ret == null ? null : ret.getByteBuf();
            }
        }

        @Override
        public void handle(Buffer event) {
            synchronized (VertxBlockingInput.this) {
                if (input1 == null) {
                    input1 = event;
                } else {
                    if (inputOverflow == null) {
                        inputOverflow = new ArrayDeque<>();
                    }
                    inputOverflow.add(event);
                }
                if (waiting) {
                    VertxBlockingInput.this.notifyAll();
                }
            }
        }

        public int readBytesAvailable() {
            if (input1 != null) {
                return input1.getByteBuf().readableBytes();
            }

            String length = request.getHeader(HttpHeaders.CONTENT_LENGTH);

            if (length == null) {
                return 0;
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
