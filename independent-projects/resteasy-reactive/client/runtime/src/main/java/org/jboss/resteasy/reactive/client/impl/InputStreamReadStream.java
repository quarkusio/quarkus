package org.jboss.resteasy.reactive.client.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicInteger;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.buffer.impl.VertxByteBufAllocator;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.streams.ReadStream;
import io.vertx.core.streams.impl.InboundBuffer;

/**
 * Copied almost verbatim from <a href=
 * "https://github.com/fabric8io/kubernetes-client/blob/v6.10.0/httpclient-vertx/src/main/java/io/fabric8/kubernetes/client/vertx/InputStreamReadStream.java">Kubernetes
 * Client</a>
 * <p>
 * TODO: There is a chance that something like this will land in Vert.x in the future, so we should check back in the future
 */
public class InputStreamReadStream implements ReadStream<Buffer> {

    private static final int MAX_DEPTH = 8;

    private final Buffer endSentinel;
    private final Vertx vertx;
    private final InputStream is;
    private final HttpClientRequest request;
    private final int chunkSize;
    private InboundBuffer<Buffer> inboundBuffer;
    private Handler<Throwable> exceptionHandler;
    private Handler<Void> endHandler;
    private byte[] bytes;

    public InputStreamReadStream(Vertx vertx, InputStream is, HttpClientRequest request, int chunkSize) {
        this.vertx = vertx;
        this.is = is;
        this.request = request;
        this.chunkSize = chunkSize;
        endSentinel = Buffer.buffer();
    }

    @Override
    public ReadStream<Buffer> exceptionHandler(Handler<Throwable> handler) {
        exceptionHandler = handler;
        return this;
    }

    final ThreadLocal<AtomicInteger> counter = new ThreadLocal<AtomicInteger>() {
        @Override
        protected AtomicInteger initialValue() {
            return new AtomicInteger();
        }
    };

    @Override
    public ReadStream<Buffer> handler(Handler<Buffer> handler) {
        boolean start = inboundBuffer == null && handler != null;
        if (start) {
            inboundBuffer = new InboundBuffer<>(vertx.getOrCreateContext());
            inboundBuffer.drainHandler(new Handler<>() {
                @Override
                public void handle(Void event) {
                    readChunk();
                }
            });
        }
        if (handler != null) {
            inboundBuffer.handler(new Handler<>() {
                @Override
                public void handle(Buffer buff) {
                    if (buff == endSentinel) {
                        if (endHandler != null) {
                            endHandler.handle(null);
                        }
                    } else {
                        handler.handle(buff);
                    }
                }
            });
        } else {
            inboundBuffer.handler(null);
        }
        if (start) {
            readChunk();
        }
        return this;
    }

    private void readChunk() {
        AtomicInteger atomicInteger = counter.get();
        try {
            int depth = atomicInteger.getAndIncrement();
            if (depth < MAX_DEPTH) {
                readChunk2();
                return;
            }
        } finally {
            atomicInteger.decrementAndGet();
        }
        vertx.runOnContext(v -> readChunk());
    }

    private void readChunk2() {
        Future<Buffer> fut = vertx.executeBlocking(new Handler<>() {
            @Override
            public void handle(Promise<Buffer> p) {
                if (bytes == null) {
                    bytes = new byte[chunkSize];
                }
                int amount;
                try {
                    amount = is.read(bytes);
                } catch (IOException e) {
                    p.fail(e);
                    return;
                }
                if (amount == -1) {
                    p.complete();
                } else {
                    p.complete(
                            Buffer.buffer(
                                    VertxByteBufAllocator.DEFAULT.heapBuffer(amount, Integer.MAX_VALUE).writeBytes(bytes, 0,
                                            amount)));
                }
            }
        });
        fut.onComplete(new Handler<>() {
            @Override
            public void handle(AsyncResult<Buffer> ar) {
                if (ar.succeeded()) {
                    Buffer chunk = ar.result();
                    if (chunk != null) {
                        boolean writable = inboundBuffer.write(chunk);
                        if (writable) {
                            readChunk();
                        } else {
                            // Full
                        }
                    } else {
                        inboundBuffer.write(endSentinel);
                    }
                } else {
                    if (exceptionHandler != null) {
                        exceptionHandler.handle(ar.cause());
                    }
                    request.reset(0, ar.cause());
                }
            }
        });
    }

    @Override
    public ReadStream<Buffer> endHandler(Handler<Void> handler) {
        endHandler = handler;
        return this;
    }

    @Override
    public ReadStream<Buffer> pause() {
        inboundBuffer.pause();
        return this;
    }

    @Override
    public ReadStream<Buffer> resume() {
        inboundBuffer.resume();
        return this;
    }

    @Override
    public ReadStream<Buffer> fetch(long amount) {
        inboundBuffer.fetch(amount);
        return this;
    }
}
