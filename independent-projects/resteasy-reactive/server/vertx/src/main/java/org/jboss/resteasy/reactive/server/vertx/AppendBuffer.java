package org.jboss.resteasy.reactive.server.vertx;

import java.util.ArrayDeque;
import java.util.Objects;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;

/**
 * It is a bounded (direct) buffer container that can keep on accepting data till {@link #capacity} is exhausted.<br>
 * In order to keep appending on it, it can {@link #clear} and consolidate its content as a {@link ByteBuf}.
 */
final class AppendBuffer {
    private final ByteBufAllocator allocator;

    private final int minChunkSize;
    private final int capacity;
    private ByteBuf buffer;
    private ArrayDeque<ByteBuf> otherBuffers;
    private int size;

    private AppendBuffer(ByteBufAllocator allocator, int minChunkSize, int capacity) {
        this.allocator = allocator;
        this.minChunkSize = Math.min(minChunkSize, capacity);
        this.capacity = capacity;
    }

    /**
     * This buffer append data in a single eagerly allocated {@link ByteBuf}.
     */
    public static AppendBuffer eager(ByteBufAllocator allocator, int capacity) {
        return new AppendBuffer(allocator, capacity, capacity);
    }

    /**
     * This buffer append data in multiples {@link ByteBuf}s sized as each {@code len} in {@link #append}.<br>
     * The data is consolidated in a single {@link CompositeByteBuf} on {@link #clear}.
     */
    public static AppendBuffer exact(ByteBufAllocator allocator, int capacity) {
        return new AppendBuffer(allocator, 0, capacity);
    }

    /**
     * This buffer append data in multiples {@link ByteBuf}s which minimum capacity is {@code minChunkSize} or
     * as each {@code len}, if greater than it.<br>
     * The data is consolidated in a single {@link CompositeByteBuf} on {@link #clear}.
     */
    public static AppendBuffer withMinChunks(ByteBufAllocator allocator, int minChunkSize, int capacity) {
        return new AppendBuffer(allocator, minChunkSize, capacity);
    }

    private ByteBuf lastBuffer() {
        if (otherBuffers == null || otherBuffers.isEmpty()) {
            return buffer;
        }
        return otherBuffers.peekLast();
    }

    /**
     * It returns how many bytes have been appended<br>
     * If returns a value different from {@code len}, is it required to invoke {@link #clear}
     * that would refill the available capacity till {@link #capacity()}
     */
    public int append(byte[] bytes, int off, int len) {
        Objects.requireNonNull(bytes);
        if (len == 0) {
            return 0;
        }
        int alreadyWritten = 0;
        if (minChunkSize > 0) {
            var lastBuffer = lastBuffer();
            if (lastBuffer != null) {
                int availableOnLast = lastBuffer.writableBytes();
                if (availableOnLast > 0) {
                    int toWrite = Math.min(len, availableOnLast);
                    lastBuffer.writeBytes(bytes, off, toWrite);
                    size += toWrite;
                    len -= toWrite;
                    // we stop if there's no more to append
                    if (len == 0) {
                        return toWrite;
                    }
                    off += toWrite;
                    alreadyWritten = toWrite;
                }
            }
        }
        final int availableCapacity = capacity - size;
        if (availableCapacity == 0) {
            return alreadyWritten;
        }
        // we can still write some
        int toWrite = Math.min(len, availableCapacity);
        assert toWrite > 0;
        final int chunkCapacity;
        if (minChunkSize > 0) {
            // Cannot allocate less than minChunkSize, till the limit of capacity left
            chunkCapacity = Math.min(Math.max(minChunkSize, toWrite), availableCapacity);
        } else {
            chunkCapacity = toWrite;
        }
        var tmpBuf = allocator.directBuffer(chunkCapacity);
        try {
            tmpBuf.writeBytes(bytes, off, toWrite);
        } catch (Throwable t) {
            tmpBuf.release();
            throw t;
        }
        if (buffer == null) {
            buffer = tmpBuf;
        } else {
            boolean resetOthers = false;
            try {
                if (otherBuffers == null) {
                    otherBuffers = new ArrayDeque<>();
                    resetOthers = true;
                }
                otherBuffers.add(tmpBuf);
            } catch (Throwable t) {
                rollback(alreadyWritten, tmpBuf, resetOthers);
                throw t;
            }
        }
        size += toWrite;
        return toWrite + alreadyWritten;
    }

    private void rollback(int alreadyWritten, ByteBuf tmpBuf, boolean resetOthers) {
        tmpBuf.release();
        if (resetOthers) {
            otherBuffers = null;
        }
        if (alreadyWritten > 0) {
            var last = lastBuffer();
            last.writerIndex(last.writerIndex() - alreadyWritten);
            size -= alreadyWritten;
            assert last.writerIndex() > 0;
        }
    }

    public ByteBuf clear() {
        var firstBuf = buffer;
        if (firstBuf == null) {
            return null;
        }
        var others = otherBuffers;
        if (others == null || others.isEmpty()) {
            size = 0;
            buffer = null;
            // super fast-path
            return firstBuf;
        }
        return clearBuffers();
    }

    private CompositeByteBuf clearBuffers() {
        var firstBuf = buffer;
        var others = otherBuffers;
        var batch = allocator.compositeDirectBuffer(1 + others.size());
        try {
            buffer = null;
            size = 0;
            batch.addComponent(true, 0, firstBuf);
            for (int i = 0, othersCount = others.size(); i < othersCount; i++) {
                // if addComponent fail, it takes care of releasing curr and throwing the exception:
                batch.addComponent(true, 1 + i, others.poll());
            }
            return batch;
        } catch (Throwable anyError) {
            batch.release();
            releaseOthers(others);
            throw anyError;
        }
    }

    private static void releaseOthers(ArrayDeque<ByteBuf> others) {
        ByteBuf buf;
        while ((buf = others.poll()) != null) {
            buf.release();
        }
    }

    public int capacity() {
        return capacity;
    }

    public int availableCapacity() {
        return capacity - size;
    }

}
