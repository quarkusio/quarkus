package io.quarkus.undertow.runtime;

import io.netty.buffer.ByteBuf;
import io.undertow.httpcore.BufferAllocator;
import io.vertx.core.net.impl.PartialPooledByteBufAllocator;

class UndertowBufferAllocator implements BufferAllocator {

    private final boolean defaultDirectBuffers;
    private final int defaultBufferSize;

    UndertowBufferAllocator(boolean defaultDirectBuffers, int defaultBufferSize) {
        this.defaultDirectBuffers = defaultDirectBuffers;
        this.defaultBufferSize = defaultBufferSize;
    }

    @Override
    public ByteBuf allocateBuffer() {
        return allocateBuffer(defaultDirectBuffers);
    }

    @Override
    public ByteBuf allocateBuffer(boolean direct) {
        if (direct) {
            return PartialPooledByteBufAllocator.DEFAULT.directBuffer(defaultBufferSize);
        } else {
            return PartialPooledByteBufAllocator.DEFAULT.heapBuffer(defaultBufferSize);
        }
    }

    @Override
    public ByteBuf allocateBuffer(int bufferSize) {
        return allocateBuffer(defaultDirectBuffers, bufferSize);
    }

    @Override
    public ByteBuf allocateBuffer(boolean direct, int bufferSize) {
        if (direct) {
            return PartialPooledByteBufAllocator.DEFAULT.directBuffer(bufferSize);
        } else {
            return PartialPooledByteBufAllocator.DEFAULT.heapBuffer(bufferSize);
        }
    }

    @Override
    public int getBufferSize() {
        return defaultBufferSize;
    }
}
