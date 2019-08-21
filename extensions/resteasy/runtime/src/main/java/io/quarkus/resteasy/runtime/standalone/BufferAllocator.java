package io.quarkus.resteasy.runtime.standalone;

import io.netty.buffer.ByteBuf;

public interface BufferAllocator {

    ByteBuf allocateBuffer();

    ByteBuf allocateBuffer(boolean direct);

    ByteBuf allocateBuffer(int bufferSize);

    ByteBuf allocateBuffer(boolean direct, int bufferSize);

    int getBufferSize();
}
