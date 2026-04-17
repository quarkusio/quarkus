package io.quarkus.vertx.http.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.ClosedChannelException;
import java.util.function.Supplier;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.quarkus.runtime.BlockingOperationNotAllowedException;
import io.quarkus.vertx.utils.VertxBlockingInput;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.net.impl.ConnectionBase;
import io.vertx.ext.web.RoutingContext;

public class VertxInputStream extends InputStream {

    public static final String CONTINUE = "100-continue";
    private final VertxBlockingInput exchange;
    private final HttpServerRequest request;

    private boolean closed;
    private boolean finished;
    private ByteBuf pooled;
    private final long limit;
    private ContinueState continueState = ContinueState.NONE;

    public VertxInputStream(RoutingContext request, long timeout) {
        this.request = request.request();
        this.exchange = createBlockingInput(this.request, timeout);
        Long limitObj = request.get(VertxHttpRecorder.MAX_REQUEST_SIZE_KEY);
        if (limitObj == null) {
            limit = -1;
        } else {
            limit = limitObj;
        }
        String expect = this.request.getHeader(HttpHeaderNames.EXPECT);
        if (expect != null && expect.equalsIgnoreCase(CONTINUE)) {
            continueState = ContinueState.REQUIRED;
        }
    }

    public VertxInputStream(RoutingContext request, long timeout, ByteBuf existing) {
        this.request = request.request();
        this.exchange = createBlockingInput(this.request, timeout);
        Long limitObj = request.get(VertxHttpRecorder.MAX_REQUEST_SIZE_KEY);
        if (limitObj == null) {
            limit = -1;
        } else {
            limit = limitObj;
        }
        this.pooled = existing;
    }

    private static VertxBlockingInput createBlockingInput(HttpServerRequest request, long timeout) {
        boolean handleHttp2Eof = request.version() == HttpVersion.HTTP_2;
        Runnable onTimeout = new Runnable() {
            @Override
            public void run() {
                request.connection().close();
            }
        };
        ConnectionBase connection = (ConnectionBase) request.connection();
        if (!connection.channel().isOpen()) {
            return new VertxBlockingInput(request, timeout, onTimeout, handleHttp2Eof,
                    new Supplier<RuntimeException>() {
                        @Override
                        public RuntimeException get() {
                            return new BlockingOperationNotAllowedException("Attempting a blocking read on io thread");
                        }
                    },
                    new ClosedChannelException());
        }
        return new VertxBlockingInput(request, timeout, onTimeout, handleHttp2Eof,
                new Supplier<RuntimeException>() {
                    @Override
                    public RuntimeException get() {
                        return new BlockingOperationNotAllowedException("Attempting a blocking read on io thread");
                    }
                }, null);
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
        if (continueState == ContinueState.REQUIRED) {
            continueState = ContinueState.SENT;
            request.response().writeContinue();
        }
        readIntoBuffer();
        if (limit > 0 && request.bytesRead() > limit) {
            HttpServerResponse response = request.response();
            if (response.headWritten()) {
                //the response has been written, not much we can do
                request.connection().close();
                throw new IOException("Request too large");
            } else {
                response.setStatusCode(HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE.code());
                response.headers().add(HttpHeaderNames.CONNECTION, "close");
                response.endHandler(new Handler<Void>() {
                    @Override
                    public void handle(Void event) {
                        request.connection().close();
                    }
                });
                response.end();
                throw new IOException("Request too large");
            }
        }
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

        int buffered = exchange.readBytesAvailable();
        if (buffered > 0) {
            return buffered;
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

    enum ContinueState {
        NONE,
        REQUIRED,
        SENT;
    }
}
