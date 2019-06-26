package io.quarkus.runtime.execution;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.Condition;

import org.wildfly.common.Assert;
import org.wildfly.common.lock.ExtendedLock;
import org.wildfly.common.lock.Locks;

import io.quarkus.runtime.Timing;

/**
 *
 */
public final class Execution {

    private static final int ASYNC_BIT /*     */ = 0b1_0000_0000_0000;
    private static final int STATUS_MASK /*   */ = 0b0_1111_0000_0000;
    private static final int EXIT_CODE_MASK /**/ = 0b0_0000_1111_1111;

    /**
     * No instances.
     */
    private Execution() {
    }

    // initial state
    static final int STATUS_IDLE = 0;
    // starting state(s)
    static final int STATUS_START = 1;
    // exit requested during start
    static final int STATUS_START_EXIT = 2;
    // asynchronous start
    static final int STATUS_START_WAIT = 3;
    // running state(s)
    static final int STATUS_RUN = 4;
    // normal exit in progress
    static final int STATUS_EXIT = 5;
    // exit via System.exit()
    static final int STATUS_ASYNC_EXIT = 6;
    // asynchronous exited state - wait for result code to be read
    static final int STATUS_EXIT_WAIT = 7;
    // dead (cannot restart ever)
    static final int STATUS_DEAD = 8;

    /**
     * Exit code indicating clean shutdown.
     */
    public static final int EXIT_OK = 0;
    /**
     * Exit code indicating a configuration or usage problem.
     */
    public static final int EXIT_CONFIG_ERROR = 1;
    /**
     * Exit code indicating an uncaught exception.
     */
    public static final int EXIT_EXCEPTION = 2;
    /**
     * Exit code indicating that shutdown did not proceed cleanly.
     */
    public static final int EXIT_UNCLEAN_SHUTDOWN = 3;

    // Code 10 is reserved due to legacy meaning with JBoss
    /**
     * Exit code indicating that the process should reload without exiting.
     */
    public static final int EXIT_RELOAD_FULL = 11;
    /**
     * Exit code indicating that the process should partially reload without shutting down listeners or thread pools.
     */
    public static final int EXIT_RELOAD_PARTIAL = 12;
    /**
     * Exit code indicating that the program was terminated by a signal, whose numeric value
     * is added to this constant as the return code.
     */
    public static final int EXIT_SIGNAL_BASE = 128;

    /**
     * Current state. Bit values:
     * <ul>
     * <li>Bit 11: Async startup indicator</li>
     * <li>Bits 10..8: Current status</li>
     * <li>Bits 7..0: Exit code</li>
     * </ul>
     *
     * Concurrency control:
     * <ul>
     * <li>Reads: no lock needed</li>
     * <li>Writes: must hold {@link #stateLock}</li>
     * </ul>
     */
    private static volatile int state = withStatus(0, STATUS_IDLE);

    /**
     * Async start/stop thrown exception.
     */
    static volatile Throwable thrown;

    /**
     * Lock protecting updates to the state.
     */
    private static final ExtendedLock stateLock = Locks.reentrantLock();
    /**
     * Condition signalled when the server has been requested to stop.
     */
    private static final Condition stopCond = stateLock.newCondition();
    /**
     * Condition signalled when the server has terminated (left {@link #STATUS_EXIT} or {@link #STATUS_ASYNC_EXIT}).
     */
    private static final Condition termCond = stateLock.newCondition();
    /**
     * Condition signalled when the server is up.
     */
    private static final Condition upCond = stateLock.newCondition();
    private static final Condition asyncCond = stateLock.newCondition();

    /**
     * Asynchronously request the server to exit with the given status code.
     *
     * @param exitCode the exit code to return (must be in the range {@code 0 ≤ n ≤ 255})
     * @return {@code true} if the exit status was successfully requested, or {@code false} if exit is already in progress
     *         or if the server is not running
     */
    public static boolean requestExit(int exitCode) {
        Assert.checkMinimumParameter("status", 0, exitCode);
        Assert.checkMaximumParameter("status", 255, exitCode);
        final ExtendedLock lock = stateLock;
        lock.lock();
        try {
            int oldVal;
            int status;
            for (;;) {
                oldVal = state;
                status = statusOf(oldVal);
                if (status == STATUS_RUN) {
                    state = withExitCode(withStatus(oldVal, STATUS_EXIT), exitCode);
                    stopCond.signalAll();
                    return true;
                } else if (status == STATUS_START) {
                    state = withExitCode(withStatus(oldVal, STATUS_START_EXIT), exitCode);
                    return true;
                } else if (status == STATUS_START_WAIT) {
                    asyncCond.awaitUninterruptibly();
                } else {
                    return false;
                }
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Asynchronously request the server to exit with a status code of {@link #EXIT_OK}.
     *
     * @return {@code true} if the exit status was successfully requested, or {@code false} if exit is already in progress
     */
    public static boolean requestShutdown() {
        return requestExit(EXIT_OK);
    }

    /**
     * Asynchronously request the server to exit with a status code of {@link #EXIT_RELOAD_FULL}.
     *
     * @return {@code true} if the exit status was successfully requested, or {@code false} if exit is already in progress
     */
    public static boolean requestReload() {
        return requestExit(EXIT_RELOAD_FULL);
    }

    /**
     * Wait for an asynchronously started server to finish its exit process, returning the exit code. This method
     * should only be called after {@link Execution#requestExit(int)} was called after
     * {@link ExecutionChain#startAsynchronously(io.quarkus.runtime.execution.ExecutionContext)}
     * was called.
     *
     * @return the exit code where {@code 0 ≤ n ≤ 255}, or {@code -1} if the server restarted without exiting
     */
    public static int awaitExit() throws ExecutionException, AsynchronousExitException {
        final ExtendedLock lock = stateLock;
        lock.lock();
        try {
            int oldVal;
            int status;
            for (;;) {
                oldVal = state;
                status = statusOf(oldVal);
                if (status == STATUS_EXIT_WAIT) {
                    // exit complete
                    state = withStatus(0, STATUS_IDLE);
                    asyncCond.signalAll();
                    Throwable thrown = Execution.thrown;
                    if (thrown != null) {
                        // automatically reap and throw
                        Execution.thrown = null;
                        throw new ExecutionException(thrown);
                    }
                    return exitCodeOf(oldVal);
                } else if (status == STATUS_EXIT || status == STATUS_START || status == STATUS_START_EXIT) {
                    // waiting
                    termCond.awaitUninterruptibly();
                } else if (status == STATUS_ASYNC_EXIT || status == STATUS_DEAD) {
                    throw asyncExitException();
                } else if (status == STATUS_RUN) {
                    return -1;
                } else if (status == STATUS_START_WAIT) {
                    state = withStatus(oldVal, STATUS_RUN);
                    asyncCond.signalAll();
                    return -1;
                } else if (status == STATUS_IDLE) {
                    throw new IllegalStateException("Server is not running");
                }
            }
        } finally {
            lock.unlock();
        }
    }

    static void awaitServerUp() throws ExecutionException, AsynchronousExitException {
        final ExtendedLock lock = stateLock;
        lock.lock();
        try {
            int oldVal;
            int status;
            for (;;) {
                oldVal = state;
                status = statusOf(oldVal);
                if (status == STATUS_START_WAIT) {
                    state = withStatus(oldVal, STATUS_RUN);
                    asyncCond.signalAll();
                    // started OK
                    return;
                } else if (status == STATUS_ASYNC_EXIT || status == STATUS_DEAD) {
                    throw asyncExitException();
                } else if (status == STATUS_EXIT) {
                    // we have to wait for the exit to complete
                    termCond.awaitUninterruptibly();
                } else if (status == STATUS_EXIT_WAIT) {
                    // server never came up or exited immediately
                    Throwable thrown = Execution.thrown;
                    if (thrown != null) {
                        // automatically reap and throw
                        state = withStatus(0, STATUS_IDLE);
                        asyncCond.signalAll();
                        Execution.thrown = null;
                        throw new ExecutionException(thrown);
                    }
                    // just act like the server came up
                    return;
                } else if (status == STATUS_START || status == STATUS_START_EXIT) {
                    upCond.awaitUninterruptibly();
                } else if (status == STATUS_IDLE || status == STATUS_RUN) {
                    throw new IllegalStateException("Server is not starting");
                } else {
                    throw unexpectedState();
                }
            }
        } finally {
            lock.unlock();
        }
    }

    static void firstStart(final boolean async) throws AsynchronousExitException {
        final ExtendedLock lock = stateLock;
        lock.lock();
        try {
            int oldVal = state;
            int status = statusOf(oldVal);
            if (status == STATUS_ASYNC_EXIT || status == STATUS_DEAD) {
                Timing.markFirstStart();
                Timing.markStop();
                throw asyncExitException();
            } else if (status != STATUS_IDLE) {
                throw new IllegalStateException("A Quarkus instance is already running");
            }
            if (async) {
                oldVal = withAsync(oldVal);
            }
            state = withStatus(oldVal, STATUS_START);
            Timing.markFirstStart();
            return;
        } finally {
            lock.unlock();
        }
    }

    static int starting() throws AsynchronousExitException {
        int status = statusOf(state);
        if (status == STATUS_START) {
            return -1;
        } else if (status == STATUS_ASYNC_EXIT || status == STATUS_DEAD) {
            throw asyncExitException();
        } else if (status == STATUS_RUN || status == STATUS_EXIT_WAIT) {
            throw unexpectedState();
        }
        final ExtendedLock lock = stateLock;
        lock.lock();
        try {
            int oldVal = state;
            status = statusOf(oldVal);
            if (status == STATUS_START) {
                return -1;
            } else if (status == STATUS_ASYNC_EXIT || status == STATUS_DEAD) {
                Timing.markStop();
                throw asyncExitException();
            } else if (status == STATUS_EXIT) {
                // restart
                state = withExitCode(withStatus(oldVal, STATUS_START), 0);
                Timing.markStart();
                termCond.signalAll();
                return -1;
            } else if (status == STATUS_RUN || status == STATUS_EXIT_WAIT) {
                throw unexpectedState();
            } else if (status == STATUS_START_EXIT) {
                // exit requested
                state = withStatus(oldVal, STATUS_EXIT);
                return exitCodeOf(oldVal);
            } else {
                throw unexpectedState();
            }
        } finally {
            lock.unlock();
        }
    }

    private static Error unexpectedState() {
        return new Error("Unexpected state");
    }

    static void stopping(int exitCode) throws AsynchronousExitException {
        int oldVal = state;
        int status = statusOf(oldVal);
        if (status == STATUS_EXIT && exitCodeOf(oldVal) == exitCode) {
            return;
        }
        if (status == STATUS_ASYNC_EXIT) {
            throw asyncExitException();
        }
        final ExtendedLock lock = stateLock;
        lock.lock();
        try {
            oldVal = state;
            status = statusOf(oldVal);
            if (status == STATUS_EXIT && exitCodeOf(oldVal) == exitCode) {
                return;
            } else if (status == STATUS_ASYNC_EXIT) {
                throw asyncExitException();
            } else if (status == STATUS_START || status == STATUS_START_EXIT) {
                Timing.markStop();
                upCond.signalAll();
                state = withExitCode(withStatus(oldVal, STATUS_EXIT), exitCode);
                return;
            } else if (status == STATUS_RUN) {
                Timing.markStop();
                state = withExitCode(withStatus(oldVal, STATUS_EXIT), exitCode);
                return;
            } else if (status == STATUS_EXIT) {
                state = withExitCode(oldVal, exitCode);
                return;
            } else {
                throw unexpectedState();
            }
        } finally {
            lock.unlock();
        }
    }

    static <E extends Throwable> E stoppingException(final E ex) throws AsynchronousExitException {
        int oldVal = state;
        int status = statusOf(oldVal);
        if (status == STATUS_EXIT && (exitCodeOf(oldVal) == EXIT_EXCEPTION || exitCodeOf(oldVal) == EXIT_UNCLEAN_SHUTDOWN)) {
            return ex;
        }
        if (status == STATUS_ASYNC_EXIT) {
            if (ex instanceof AsynchronousExitException) {
                return ex;
            } else {
                throw asyncExitException(ex);
            }
        }
        final ExtendedLock lock = stateLock;
        lock.lock();
        try {
            oldVal = state;
            status = statusOf(oldVal);
            if (status == STATUS_EXIT
                    && (exitCodeOf(oldVal) == EXIT_EXCEPTION || exitCodeOf(oldVal) == EXIT_UNCLEAN_SHUTDOWN)) {
                return ex;
            }
            if (status == STATUS_ASYNC_EXIT) {
                if (ex instanceof AsynchronousExitException) {
                    return ex;
                } else {
                    throw asyncExitException(ex);
                }
            } else if (status == STATUS_START || status == STATUS_START_EXIT) {
                Timing.markStop();
                state = withExitCode(withStatus(oldVal, STATUS_EXIT), EXIT_EXCEPTION);
                upCond.signalAll();
                return ex;
            } else if (status == STATUS_RUN) {
                Timing.markStop();
                state = withExitCode(withStatus(oldVal, STATUS_EXIT), EXIT_EXCEPTION);
                return ex;
            } else if (status == STATUS_EXIT && exitCodeOf(oldVal) == EXIT_OK) {
                state = withStatus(withExitCode(oldVal, EXIT_UNCLEAN_SHUTDOWN), STATUS_EXIT);
                return ex;
            } else if (status == STATUS_EXIT) {
                state = withExitCode(withStatus(oldVal, STATUS_EXIT), EXIT_EXCEPTION);
                return ex;
            } else {
                throw unexpectedState();
            }
        } finally {
            lock.unlock();
        }
    }

    static void lastStop() {
        final ExtendedLock lock = stateLock;
        lock.lock();
        try {
            int oldVal = state;
            int status = statusOf(oldVal);
            if (status == STATUS_EXIT) {
                if (isAsync(oldVal)) {
                    state = withStatus(oldVal, STATUS_EXIT_WAIT);
                } else {
                    state = withStatus(0, STATUS_IDLE);
                }
                termCond.signalAll();
                return;
            } else if (status == STATUS_ASYNC_EXIT) {
                state = withStatus(0, STATUS_DEAD);
                termCond.signalAll();
            }
        } finally {
            lock.unlock();
        }
    }

    static int signalUpAndAwaitStop() throws AsynchronousExitException {
        final ExtendedLock lock = stateLock;
        lock.lock();
        try {
            int oldVal = state;
            int status = statusOf(oldVal);
            if (status == STATUS_START) {
                if (isAsync(oldVal)) {
                    state = withStatus(oldVal, status = STATUS_START_WAIT);
                } else {
                    state = withStatus(oldVal, status = STATUS_RUN);
                }
                // signal that we've come up
                upCond.signalAll();
            } else if (status == STATUS_START_EXIT) {
                // we shut down before we started up
                Timing.markStop();
                state = withStatus(oldVal, STATUS_EXIT);
                upCond.signalAll();
                return exitCodeOf(oldVal);
            } else if (status == STATUS_EXIT) {
                // we shut down before we started up
                Timing.markStop();
                upCond.signalAll();
                return exitCodeOf(oldVal);
            } else if (status == STATUS_ASYNC_EXIT) {
                // we shut down before we started up
                Timing.markStop();
                upCond.signalAll();
                throw asyncExitException();
            } else {
                throw unexpectedState();
            }
            for (;;) {
                assert status == STATUS_RUN || status == STATUS_START_WAIT;
                // wait for someone to request termination
                stopCond.awaitUninterruptibly();
                oldVal = state;
                status = statusOf(oldVal);
                if (status == STATUS_ASYNC_EXIT) {
                    Timing.markStop();
                    throw asyncExitException();
                } else if (status == STATUS_EXIT) {
                    Timing.markStop();
                    return exitCodeOf(oldVal);
                } else if (status == STATUS_RUN || status == STATUS_START_WAIT) {
                    // spurious wakeup
                } else {
                    throw unexpectedState();
                }
            }
        } finally {
            lock.unlock();
        }
    }

    static void signalAsyncExitAndAwaitTerm() {
        final ExtendedLock lock = stateLock;
        lock.lock();
        try {
            int oldVal = state;
            int status = statusOf(oldVal);
            if (status == STATUS_ASYNC_EXIT) {
                // already signalled; do nothing
            } else if (status == STATUS_START) {
                Timing.markStop();
                state = withStatus(0, status = STATUS_ASYNC_EXIT);
                upCond.signalAll();
            } else if (status == STATUS_RUN) {
                Timing.markStop();
                state = withStatus(0, status = STATUS_ASYNC_EXIT);
                stopCond.signalAll();
            } else if (status == STATUS_EXIT) {
                state = withStatus(0, status = STATUS_ASYNC_EXIT);
                // no signal
            } else if (status == STATUS_EXIT_WAIT || status == STATUS_IDLE) {
                state = withStatus(0, STATUS_DEAD);
                // we can shut down immediately because nothing else can be done or returned
                return;
            } else if (status == STATUS_DEAD) {
                // unexpected, but no reason to fail because of it
                return;
            }
            // now await shutdown
            for (;;) {
                assert status == STATUS_ASYNC_EXIT;
                termCond.awaitUninterruptibly();
                oldVal = state;
                status = statusOf(oldVal);
                if (status == STATUS_ASYNC_EXIT) {
                    // spurious wakeup
                } else if (status == STATUS_DEAD) {
                    return;
                }
            }
        } finally {
            lock.unlock();
        }
    }

    static void signalAsyncExit() {
        final ExtendedLock lock = stateLock;
        lock.lock();
        try {
            int oldVal = state;
            int status = statusOf(oldVal);
            if (status == STATUS_ASYNC_EXIT) {
                // already signalled; do nothing
            } else if (status == STATUS_START) {
                Timing.markStop();
                state = withStatus(0, STATUS_ASYNC_EXIT);
                upCond.signalAll();
            } else if (status == STATUS_RUN) {
                Timing.markStop();
                state = withStatus(0, STATUS_ASYNC_EXIT);
                stopCond.signalAll();
            } else if (status == STATUS_EXIT) {
                state = withStatus(0, STATUS_ASYNC_EXIT);
                // no signal
            } else if (status == STATUS_EXIT_WAIT || status == STATUS_IDLE) {
                state = withStatus(0, STATUS_DEAD);
                // we can shut down immediately because nothing else can be done or returned
                return;
            } else if (status == STATUS_DEAD) {
                // unexpected, but no reason to fail because of it
                return;
            }
        } finally {
            lock.unlock();
        }
    }

    private static AsynchronousExitException asyncExitException() {
        return new AsynchronousExitException("Application was asynchronously exited");
    }

    private static AsynchronousExitException asyncExitException(final Throwable cause) {
        final AsynchronousExitException ex = asyncExitException();
        if (cause != null) {
            ex.initCause(cause);
        }
        return ex;
    }

    private static int withStatus(int state, int status) {
        return state & ~STATUS_MASK | status << 8;
    }

    private static int withAsync(int state) {
        return state | ASYNC_BIT;
    }

    private static int withExitCode(int state, int code) {
        assert 0 <= code && code <= EXIT_CODE_MASK;
        return state & ~EXIT_CODE_MASK | code;
    }

    private static int statusOf(int state) {
        return (state & STATUS_MASK) >>> 8;
    }

    private static boolean isAsync(int state) {
        return (state & ASYNC_BIT) != 0;
    }

    private static int exitCodeOf(int state) {
        return state & EXIT_CODE_MASK;
    }
}
