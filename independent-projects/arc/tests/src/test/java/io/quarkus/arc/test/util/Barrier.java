package io.quarkus.arc.test.util;

import java.util.concurrent.locks.AbstractQueuedSynchronizer;

// adapted from `AbstractQueuedSynchronizer`
public final class Barrier {
    private static class Sync extends AbstractQueuedSynchronizer {
        protected int tryAcquireShared(int ignore) {
            return getState() != 0 ? 1 : -1;
        }

        protected boolean tryReleaseShared(int ignore) {
            setState(1);
            return true;
        }
    }

    private final Sync sync = new Sync();

    public void await() throws InterruptedException {
        sync.acquireSharedInterruptibly(1);
    }

    public void open() {
        sync.releaseShared(1);
    }
}
