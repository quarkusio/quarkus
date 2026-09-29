package io.quarkus.aesh.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.aesh.terminal.AbstractConnection;
import org.aesh.terminal.Attributes;
import org.aesh.terminal.BaseDevice;
import org.aesh.terminal.Device;
import org.aesh.terminal.EventDecoder;
import org.aesh.terminal.tty.Capability;
import org.aesh.terminal.tty.Size;
import org.aesh.terminal.utils.Parser;
import org.jboss.logging.Logger;

/**
 * A {@link org.aesh.terminal.Connection} backed by JDK {@link InputStream}/{@link OutputStream} pairs.
 * <p>
 * Extends {@link AbstractConnection} to leverage its {@link EventDecoder} which
 * buffers input in a queue when no {@code stdinHandler} is set. This prevents
 * input loss during the window between readline cycles where the handler is
 * temporarily null (see <a href="https://github.com/aeshell/aesh-readline/issues/233">aesh-readline#233</a>).
 * <p>
 * Used by {@link CliRunner} when running in test mode. The test framework
 * provides the streams via {@link AeshTestConnectionHolder}, and this
 * class wraps them into a proper aesh Connection -- all within the
 * runtime classloader, avoiding cross-classloader type issues.
 */
class AeshStreamConnection extends AbstractConnection {

    /**
     * Marker for reader-death signals offered on the signal queue:
     * {@code Object[] { READER_DEATH_MARKER, cause }}. Must stay in sync
     * with the structural check in {@code AeshLauncherImpl} (separate
     * classloader, literal intentionally duplicated there).
     */
    static final String READER_DEATH_MARKER = "aesh-test-reader-death";

    private static final Logger LOG = Logger.getLogger(AeshStreamConnection.class);

    private final Device device = new BaseDevice("test");
    private final Size size = new Size(120, 40);
    private final InputStream input;
    private final OutputStream output;
    private final LinkedBlockingQueue<Object> signalQueue;
    private final AtomicReference<Throwable> readerDeath;
    private final AtomicLong lastReadlineArmNanos;
    private final AtomicLong connectionCloseNanos;
    private final AtomicLong armCount;

    private volatile boolean closed = false;
    private Thread readerThread;

    AeshStreamConnection(InputStream input, OutputStream output,
            LinkedBlockingQueue<Object> signalQueue, AtomicReference<Throwable> readerDeath,
            AtomicLong lastReadlineArmNanos, AtomicLong connectionCloseNanos, AtomicLong armCount) {
        this.input = input;
        this.output = output;
        this.signalQueue = signalQueue;
        this.readerDeath = readerDeath;
        this.lastReadlineArmNanos = lastReadlineArmNanos;
        this.connectionCloseNanos = connectionCloseNanos;
        this.armCount = armCount;
        this.attributes = new Attributes();
        this.eventDecoder = new EventDecoder(this.attributes);
        this.stdout = data -> {
            try {
                String text = Parser.fromCodePoints(data);
                output.write(text.getBytes(StandardCharsets.UTF_8));
                output.flush();
            } catch (IOException e) {
                // Connection closed
            }
        };
    }

    @Override
    public Device device() {
        return device;
    }

    @Override
    public Size size() {
        return size;
    }

    @Override
    public void close() {
        closed = true;
        if (connectionCloseNanos != null) {
            connectionCloseNanos.compareAndSet(0, System.nanoTime());
        }
        LOG.infof("aesh-test: connection closed");
        // Close the input stream to unblock the reader thread
        try {
            input.close();
        } catch (IOException e) {
            // Ignore
        }
        if (closeHandler != null) {
            closeHandler.accept(null);
        }
    }

    @Override
    public void openBlocking() {
        startReader();
        try {
            if (readerThread != null) {
                readerThread.join();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void openNonBlocking() {
        startReader();
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

    @Override
    public boolean put(Capability capability, Object... params) {
        return false;
    }

    @Override
    public Charset inputEncoding() {
        return StandardCharsets.UTF_8;
    }

    @Override
    public Charset outputEncoding() {
        return StandardCharsets.UTF_8;
    }

    @Override
    public boolean supportsAnsi() {
        return false;
    }

    @Override
    public boolean isInteractive() {
        // Report as interactive so aesh does not switch to synchronous mode.
        // The test framework simulates an interactive session via piped streams.
        return true;
    }

    private void startReader() {
        if (readerThread != null) {
            return;
        }
        readerThread = new Thread(() -> {
            byte[] buffer = new byte[1024];
            try {
                while (!closed) {
                    int n = input.read(buffer);
                    if (n == -1) {
                        break;
                    }
                    if (n > 0) {
                        String text = new String(buffer, 0, n, StandardCharsets.UTF_8);
                        // Deliver input via EventDecoder which buffers in its
                        // inputQueue when stdinHandler is null, preventing
                        // input loss between readline cycles.
                        eventDecoder.accept(Parser.toCodePoints(text));
                    }
                }
            } catch (IOException e) {
                // Stream closed, exit reader
            } catch (Throwable t) {
                // Anything else kills input delivery silently and every later
                // command hangs: record the cause where the test side can
                // report it, and wake any waiter immediately.
                if (t instanceof ThreadDeath) {
                    throw (ThreadDeath) t;
                }
                if (readerDeath != null) {
                    readerDeath.compareAndSet(null, t);
                }
                LOG.errorf(t, "aesh-test-reader died unexpectedly; REPL input will no longer be delivered");
                if (signalQueue != null) {
                    try {
                        signalQueue.offer(new Object[] { READER_DEATH_MARKER, t });
                    } catch (Throwable ignored) {
                        // Best effort only; the sticky record above is authoritative
                    }
                }
            }
        }, "aesh-test-reader");
        readerThread.setDaemon(true);
        readerThread.start();
    }
}
