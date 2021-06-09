package io.quarkus.dev.testing;

import java.util.concurrent.locks.ReentrantLock;

/**
 * lock that is used to prevent scanning while the dev mode test is updating classes
 *
 * This prevents races in the continuous testing tests. It's not an ideal solution
 * but its the only one I can think of at the moment.
 */
public class TestScanningLock {

    private static final ReentrantLock lock = new ReentrantLock();

    /**
     * There is a race when testing this, where you can see the intermediate empty state of the
     * file, or where the file time changes twice. Dev mode tests hold this lock during modification
     * to avoid the race.
     */
    public static void lockForTests() {
        lock.lock();
    }

    public static void unlockForTests() {
        lock.unlock();
    }
}
