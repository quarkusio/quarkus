package io.quarkus.jackson.runtime;

import com.fasterxml.jackson.core.util.BufferRecycler;
import com.fasterxml.jackson.core.util.BufferRecyclerPool;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Predicate;

public class HybridJacksonPool implements BufferRecyclerPool {

    static final BufferRecyclerPool INSTANCE = new HybridJacksonPool();

    private static final Predicate<Thread> isVirtual = VirtualPredicate.findIsVirtualPredicate();

    private final BufferRecyclerPool nativePool = BufferRecyclerPool.threadLocalPool();

    static class VirtualPoolHolder {
        // Lazy on-demand initialization
        private static final BufferRecyclerPool virtualPool = new StripedLockFreePool(4);
    }

    @Override
    public BufferRecycler acquireBufferRecycler() {
        return isVirtual.test(Thread.currentThread()) ?
                VirtualPoolHolder.virtualPool.acquireBufferRecycler() :
                nativePool.acquireBufferRecycler();
    }

    @Override
    public void releaseBufferRecycler(BufferRecycler bufferRecycler) {
        if (bufferRecycler instanceof VThreadBufferRecycler) {
            // if it is a PooledBufferRecycler it has been acquired by a virtual thread, so it has to be release to the same pool
            VirtualPoolHolder.virtualPool.releaseBufferRecycler(bufferRecycler);
        }
        // the native thread pool is based on ThreadLocal, so it doesn't have anything to do on release
    }

    private static class StripedLockFreePool implements BufferRecyclerPool {

        private static final int CACHE_LINE_SHIFT = 4;

        private static final int CACHE_LINE_PADDING = 1 << CACHE_LINE_SHIFT;

        private final XorShiftThreadProbe threadProbe;

        private final AtomicReferenceArray<Node> heads;

        public StripedLockFreePool(int stripesCount) {
            if (stripesCount <= 0) {
                throw new IllegalArgumentException("Expecting a stripesCount that is larger than 0");
            }

            int size = roundToPowerOfTwo(stripesCount);
            this.heads = new AtomicReferenceArray<>(size * CACHE_LINE_PADDING);

            int mask = (size - 1) << CACHE_LINE_SHIFT;
            this.threadProbe = new XorShiftThreadProbe(mask);
        }

        @Override
        public BufferRecycler acquireBufferRecycler() {
            int index = threadProbe.index();

            Node currentHead = heads.get(index);
            while (true) {
                if (currentHead == null) {
                    return new VThreadBufferRecycler(index);
                }

                Node witness = heads.compareAndExchange(index, currentHead, currentHead.next);
                if (witness == currentHead) {
                    currentHead.next = null;
                    return currentHead.value;
                } else {
                    currentHead = witness;
                }
            }
        }

        @Override
        public void releaseBufferRecycler(BufferRecycler recycler) {
            VThreadBufferRecycler vThreadBufferRecycler = (VThreadBufferRecycler) recycler;
            Node newHead = new Node(vThreadBufferRecycler);

            Node next = heads.get(vThreadBufferRecycler.slot);
            while (true) {
                Node witness = heads.compareAndExchange(vThreadBufferRecycler.slot, next, newHead);
                if (witness == next) {
                    newHead.next = next;
                    return;
                } else {
                    next = witness;
                }
            }
        }

        private static class Node {
            final VThreadBufferRecycler value;
            Node next;

            Node(VThreadBufferRecycler value) {
                this.value = value;
            }
        }
    }

    private static class VThreadBufferRecycler extends BufferRecycler {
        private final int slot;

        VThreadBufferRecycler(int slot) {
            this.slot = slot;
        }
    }

    private static class VirtualPredicate {
        private static final MethodHandle virtualMh = findVirtualMH();

        private static MethodHandle findVirtualMH() {
            try {
                return MethodHandles.publicLookup().findVirtual(Thread.class, "isVirtual", MethodType.methodType(boolean.class));
            } catch (Exception e) {
                return null;
            }
        }

        private static Predicate<Thread> findIsVirtualPredicate() {
            return virtualMh != null ? t -> {
                try {
                    return (boolean) virtualMh.invokeExact(t);
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            } : t -> false;
        }
    }

    private static class XorShiftThreadProbe {

        private final int mask;


        XorShiftThreadProbe(int mask) {
            this.mask = mask;
        }

        public int index() {
            return probe() & mask;
        }

        private int probe() {
            int probe = (int) ((Thread.currentThread().getId() * 0x9e3779b9) & Integer.MAX_VALUE);
            // xorshift
            probe ^= probe << 13;
            probe ^= probe >>> 17;
            probe ^= probe << 5;
            return probe;
        }
    }

    private static final int MAX_POW2 = 1 << 30;

    private static int roundToPowerOfTwo(final int value) {
        if (value > MAX_POW2) {
            throw new IllegalArgumentException("There is no larger power of 2 int for value:"+value+" since it exceeds 2^31.");
        }
        if (value < 0) {
            throw new IllegalArgumentException("Given value:"+value+". Expecting value >= 0.");
        }
        final int nextPow2 = 1 << (32 - Integer.numberOfLeadingZeros(value - 1));
        return nextPow2;
    }
}
