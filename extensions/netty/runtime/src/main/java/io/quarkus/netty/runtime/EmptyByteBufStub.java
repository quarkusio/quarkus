package io.quarkus.netty.runtime;

import java.nio.ByteBuffer;

public final class EmptyByteBufStub {
    private static final ByteBuffer EMPTY_BYTE_BUFFER = ByteBuffer.allocateDirect(0);

    public static ByteBuffer emptyByteBuffer() {
        return EMPTY_BYTE_BUFFER;
    }

    private EmptyByteBufStub() {
    }
}
