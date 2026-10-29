package io.quarkus.aesh.runtime;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.aesh.terminal.StreamConnection;
import org.jboss.logging.Logger;

/**
 * Test-mode {@link StreamConnection} carrying the test framework's
 * observability hooks.
 * <p>
 * All stream pumping, decoding, buffering, and lifecycle management comes
 * from upstream's {@code StreamConnection} (daemon reader thread,
 * byte-level decoding across split reads, {@code EventDecoder} buffering
 * while no handler is set, EOF handling). This subclass only adds the
 * test-only instrumentation the framework needs to diagnose wedged
 * sessions instead of hanging on them:
 * <ul>
 * <li>reader-death reporting via {@code setReaderDeathHook}, recorded in
 * the shared sticky reference and signalled on the queue to wake any
 * waiter immediately;</li>
 * <li>readline arm/close timestamps and a handler-replacement counter
 * for the re-arm tripwire in {@code AeshLauncherImpl}.</li>
 * </ul>
 * <p>
 * Used by {@link CliRunner} when running in test mode. The test framework
 * provides the streams via {@link AeshTestConnectionHolder}, and this
 * class wraps them into a proper aesh Connection -- all within the
 * runtime classloader, avoiding cross-classloader type issues.
 */
class AeshStreamConnection extends StreamConnection {

    /**
     * Marker for reader-death signals offered on the signal queue:
     * {@code Object[] { READER_DEATH_MARKER, cause }}. Must stay in sync
     * with the structural check in {@code AeshLauncherImpl} (separate
     * classloader, literal intentionally duplicated there).
     */
    static final String READER_DEATH_MARKER = "aesh-test-reader-death";

    private static final Logger LOG = Logger.getLogger(AeshStreamConnection.class);

    private final LinkedBlockingQueue<Object> signalQueue;
    private final AtomicReference<Throwable> readerDeath;
    private final AtomicLong lastReadlineArmNanos;
    private final AtomicLong connectionCloseNanos;
    private final AtomicLong armCount;

    AeshStreamConnection(InputStream input, OutputStream output,
            LinkedBlockingQueue<Object> signalQueue, AtomicReference<Throwable> readerDeath,
            AtomicLong lastReadlineArmNanos, AtomicLong connectionCloseNanos, AtomicLong armCount) {
        super(StandardCharsets.UTF_8, input, output);
        this.signalQueue = signalQueue;
        this.readerDeath = readerDeath;
        this.lastReadlineArmNanos = lastReadlineArmNanos;
        this.connectionCloseNanos = connectionCloseNanos;
        this.armCount = armCount;
        setReaderDeathHook(this::recordReaderDeath);
    }

    @Override
    public void close() {
        if (connectionCloseNanos != null) {
            connectionCloseNanos.compareAndSet(0, System.nanoTime());
        }
        LOG.infof("aesh-test: connection closed");
        super.close();
    }

    @Override
    public void setStdinHandler(Consumer<int[]> handler) {
        super.setStdinHandler(handler);
        // Count every handler replacement (re-arms, submit stubs, scoped
        // replacements alike — all only inflate the count). The test side
        // asserts the count advances per completed command, which catches a
        // skipped post-completion re-arm at the command itself instead of as
        // a hang on the next one.
        if (armCount != null) {
            armCount.incrementAndGet();
        }
        // Timestamp every arming so hang diagnostics can tell whether
        // readline re-armed after the previous command completed. The
        // clearing half is logged for cycle correlation in CI output.
        if (handler != null) {
            if (lastReadlineArmNanos != null) {
                lastReadlineArmNanos.set(System.nanoTime());
            }
            LOG.infof("aesh-test: readline armed for input");
        } else {
            LOG.infof("aesh-test: readline disarmed (stdin handler cleared)");
        }
    }

    /**
     * Records an unexpected reader death where the test side can report it,
     * and wakes any waiter immediately via a marker signal. Upstream only
     * invokes the hook for genuine deaths (clean EOF/shutdown paths bypass
     * it), so recording here never fires on normal session teardown.
     */
    private void recordReaderDeath(Throwable cause) {
        if (readerDeath != null) {
            readerDeath.compareAndSet(null, cause);
        }
        LOG.errorf(cause, "aesh-test reader died unexpectedly; REPL input will no longer be delivered");
        if (signalQueue != null) {
            try {
                signalQueue.offer(new Object[] { READER_DEATH_MARKER, cause });
            } catch (Throwable ignored) {
                // Best effort only; the sticky record above is authoritative
            }
        }
    }
}
