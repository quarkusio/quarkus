package io.quarkus.deployment.dev;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Lock that is used to prevent scanning and compiling while code generator is updating sources
 * There is a race when testing this, where you can see the intermediate empty state of the
 * file, or where the file time changes twice. Codegen hold this lock during modification
 * to avoid the race.
 */
public class CodeGenLock {

    /**
     * Allow for multiple code generators to run at the same time but give exclusive run to RuntimeUpdatesProcessor
     */
    private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);

    public static Lock lockForCodeGen() {
        return lock.readLock();
    }

    public static Lock lockForCompilation() {
        return lock.writeLock();
    }
}
