package io.quarkus.netty.runtime;

import java.nio.ByteBuffer;

import io.netty.util.internal.PlatformDependent;

public final class EmptyByteBufStub {
    private static final ByteBuffer EMPTY_BYTE_BUFFER = ByteBuffer.allocateDirect(0);
    private static final long EMPTY_BYTE_BUFFER_ADDRESS;

    static {
        long emptyByteBufferAddress = 0;
        try {
            if (PlatformDependent.hasUnsafe()) {
                emptyByteBufferAddress = PlatformDependent.directBufferAddress(EMPTY_BYTE_BUFFER);
            }
        } catch (Throwable t) {
            // Ignore
        }
        EMPTY_BYTE_BUFFER_ADDRESS = emptyByteBufferAddress;
    }

    public static ByteBuffer emptyByteBuffer() {
        return EMPTY_BYTE_BUFFER;
    }

    public static long emptyByteBufferAddress() {
        return EMPTY_BYTE_BUFFER_ADDRESS;
    }

    private EmptyByteBufStub() {
    }
}
