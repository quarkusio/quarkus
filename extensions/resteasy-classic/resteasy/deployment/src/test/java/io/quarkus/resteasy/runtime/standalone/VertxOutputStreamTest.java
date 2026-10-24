package io.quarkus.resteasy.runtime.standalone;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Proxy;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;

/**
 * Plain unit test (no Quarkus application) for {@link VertxOutputStream}.
 */
public class VertxOutputStreamTest {

    @Test
    public void testAsyncWriteFailsWhenChunkWriteThrows() {
        AtomicInteger writes = new AtomicInteger();
        VertxHttpResponse response = new VertxHttpResponse(stubRequest(), null, HttpMethod.HEAD, null, null, null) {
            @Override
            public CompletionStage<Void> writeNonBlocking(ByteBuf buffer, boolean finished) {
                if (writes.incrementAndGet() == 1) {
                    return CompletableFuture.completedFuture(null);
                }
                throw new IllegalStateException("Response has already been written");
            }
        };
        VertxOutputStream stream = new VertxOutputStream(response, new FixedSizeAllocator(4));

        // 3 full buffers: the first chunk write succeeds, the second one throws
        CompletableFuture<Void> written = stream.asyncWrite(new byte[12], 0, 12).toCompletableFuture();

        ExecutionException e = assertThrows(ExecutionException.class, () -> written.get(5, TimeUnit.SECONDS));
        assertInstanceOf(IllegalStateException.class, e.getCause());
    }

    private static HttpServerRequest stubRequest() {
        return (HttpServerRequest) Proxy.newProxyInstance(VertxOutputStreamTest.class.getClassLoader(),
                new Class<?>[] { HttpServerRequest.class }, (proxy, method, args) -> null);
    }

    private record FixedSizeAllocator(int size) implements BufferAllocator {

        @Override
        public ByteBuf allocateBuffer() {
            return Unpooled.buffer(size, size);
        }

        @Override
        public ByteBuf allocateBuffer(boolean direct) {
            return allocateBuffer();
        }

        @Override
        public ByteBuf allocateBuffer(int bufferSize) {
            return Unpooled.buffer(bufferSize, bufferSize);
        }

        @Override
        public ByteBuf allocateBuffer(boolean direct, int bufferSize) {
            return allocateBuffer(bufferSize);
        }

        @Override
        public int getBufferSize() {
            return size;
        }
    }
}
