package io.quarkus.bootstrap.runner;

import java.util.concurrent.atomic.AtomicReference;

/**
 * A thread-safe buffer allocator that pools heap buffers.<br>
 * It's currently implemented using a concurrent linked stack, but the users shouldn't rely on how it's implemented,
 * as it's subject to change. Conversely, users can rely on the fact that it's thread-safe.
 *
 * Users which relies on its buffer API, but doesn't need anymore to pool buffers, can call {@link #disablePooling()}
 * which will make it behave as if it was disabled from the start and release any buffer that was still pooled.
 */
public final class PooledBufferAllocator {

    // This is a sentinel value used to forcibly disable pooling.
    private static final Buffer EMPTY = new Buffer();

    /**
     * A buffer that contains the data of a resource.<br>
     * It can be released back (once) to its allocator via {@link PooledBufferAllocator#release(Buffer)}.
     */
    public static final class Buffer {
        private byte[] data;
        private int length;
        private Buffer next;

        private Buffer(byte[] data, int length) {
            assert data != null && length >= 0;
            this.data = data;
            this.length = length;
        }

        private Buffer() {
        }

        public byte[] array() {
            return data;
        }

        public int length() {
            return length;
        }

        /**
         * It moves the data from this buffer to a new one, and returns the new one.
         */
        private Buffer moveDataToNewBufferOf(final int requiredCapacity) {
            byte[] data = this.data;
            // it's forbidden to modify EMPTY
            if (this != EMPTY) {
                this.data = null;
            }
            if (data == null || data.length < requiredCapacity) {
                data = new byte[requiredCapacity];
            }
            return new Buffer(data, requiredCapacity);
        }
    }

    // This is the top of the stack of buffers that are available for reuse.
    private final AtomicReference<Buffer> topStack;

    public PooledBufferAllocator() {
        topStack = new AtomicReference<>(null);
    }

    public Buffer allocate(final int requiredCapacity) {
        // to avoid ABA problems on release, we need to make sure that the buffer is not reused
        return popBuffer().moveDataToNewBufferOf(requiredCapacity);
    }

    private Buffer popBuffer() {
        AtomicReference<Buffer> top = topStack;
        Buffer topBuffer;
        do {
            topBuffer = top.get();
            if (topBuffer == EMPTY || topBuffer == null) {
                return EMPTY;
            }
        } while (!top.compareAndSet(topBuffer, topBuffer.next));
        topBuffer.next = null;
        assert topBuffer != EMPTY;
        return topBuffer;
    }

    /**
     * This method is called when a buffer obtained by {@link #allocate(int)} is no longer needed.<br>
     * It doesn't enforce belonging to this specific allocator, but the behaviour is undefined if it doesn't.
     * The same applies in case {@code buffer} is released more than once.
     */
    public void release(Buffer buffer) {
        final AtomicReference<Buffer> top = topStack;
        Buffer topBuffer;
        do {
            topBuffer = top.get();
            if (topBuffer == EMPTY) {
                return;
            }
            // implicit throw NPE, no need to check upfront
            buffer.next = topBuffer;
        } while (!top.compareAndSet(topBuffer, buffer));
    }

    public void disablePooling() {
        topStack.lazySet(EMPTY);
    }
}
