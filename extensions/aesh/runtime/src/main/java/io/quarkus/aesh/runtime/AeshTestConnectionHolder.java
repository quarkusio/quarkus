package io.quarkus.aesh.runtime;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Bridges test and runtime classloaders using the current thread's name
 * as a marker and a custom Thread subclass ({@link AeshTestThread}) to
 * carry the test channels. Both the test-side {@code AeshLauncherImpl}
 * and the runtime-side {@link CliRunner} run on the same thread, so the
 * data is accessible without any cross-classloader storage.
 * <p>
 * Previous versions stored objects in {@link System#getProperties()}, but
 * that caused {@code NullPointerException} in libraries (e.g. Narayana)
 * that iterate all system property names and call
 * {@code System.getProperty(key)} on non-String values.
 */
public final class AeshTestConnectionHolder {

    /**
     * Thread name prefix that marks a thread as carrying test channel data.
     */
    public static final String TEST_THREAD_NAME = "aesh-test-repl";

    private AeshTestConnectionHolder() {
    }

    /**
     * Retrieve the test input stream from the current thread via reflection.
     * Uses field name lookup instead of {@code instanceof} to avoid
     * classloader identity issues.
     */
    static InputStream getInput() {
        return (InputStream) getFieldFromThread("testInput");
    }

    /**
     * Retrieve the test output stream from the current thread via reflection.
     */
    static OutputStream getOutput() {
        return (OutputStream) getFieldFromThread("testOutput");
    }

    /**
     * Retrieve the signal queue from the current thread via reflection.
     */
    @SuppressWarnings("unchecked")
    static LinkedBlockingQueue<Object> getSignalQueue() {
        return (LinkedBlockingQueue<Object>) getFieldFromThread("signalQueue");
    }

    /**
     * Retrieve the command output capture stream from the current thread.
     * This stream receives only command output (from invocation.println()),
     * not readline prompt or ANSI chrome.
     */
    static OutputStream getCommandOutputCapture() {
        return (OutputStream) getFieldFromThread("commandOutputCapture");
    }

    /**
     * Retrieve the input line response queue from the current thread.
     * Pre-canned responses are consumed by {@code invocation.inputLine()}
     * for interactive command testing.
     */
    @SuppressWarnings("unchecked")
    static Queue<String> getInputLineQueue() {
        return (Queue<String>) getFieldFromThread("inputLineQueue");
    }

    /**
     * Retrieve the shared reader-death record from the current thread.
     * Recorded by {@code AeshStreamConnection} when its reader thread dies
     * unexpectedly; read by the test side to fail fast with the cause
     * instead of hanging. {@code null} when not in test mode or when the
     * thread predates reader-death support.
     */
    @SuppressWarnings("unchecked")
    static AtomicReference<Throwable> getReaderDeath() {
        return (AtomicReference<Throwable>) getFieldFromThread("readerDeath");
    }

    /**
     * Retrieve the shared readline-arm timestamp from the current thread.
     * Stamped (nanos) every time readline arms for input; 0 means never.
     * {@code null} when not in test mode or the thread predates support.
     */
    static AtomicLong getLastReadlineArmNanos() {
        return (AtomicLong) getFieldFromThread("lastReadlineArmNanos");
    }

    /**
     * Retrieve the shared connection-close timestamp from the current thread.
     * Stamped (nanos) if the connection is ever closed mid-session; 0 means
     * it never ran — i.e. no shutdown path preempted readline re-arming.
     * {@code null} when not in test mode or the thread predates support.
     */
    static AtomicLong getConnectionCloseNanos() {
        return (AtomicLong) getFieldFromThread("connectionCloseNanos");
    }

    /**
     * Retrieve the shared handler-replacement counter from the current thread.
     * {@code null} when not in test mode or the thread predates support.
     */
    static AtomicLong getArmCount() {
        return (AtomicLong) getFieldFromThread("armCount");
    }

    private static Object getFieldFromThread(String fieldName) {
        Thread t = Thread.currentThread();
        if (!TEST_THREAD_NAME.equals(t.getName())) {
            return null;
        }
        try {
            java.lang.reflect.Field f = t.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            return f.get(t);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return null;
        }
    }

    /**
     * A Thread subclass that carries test channel data. Created by
     * {@code AeshLauncherImpl} and checked by {@link CliRunner}.
     * <p>
     * Uses JDK-only field types so both classloaders see the same types.
     */
    public static class AeshTestThread extends Thread {
        final InputStream testInput;
        final OutputStream testOutput;
        final LinkedBlockingQueue<Object> signalQueue;
        final OutputStream commandOutputCapture;
        final ConcurrentLinkedQueue<String> inputLineQueue;
        // Sticky record of unexpected reader-thread death, shared with
        // AeshStreamConnection. First death wins; read by the test side to
        // fail fast with the cause instead of hanging on a dead session.
        final AtomicReference<Throwable> readerDeath = new AtomicReference<>();
        // Readline-arm timestamp (nanos, 0 = never), stamped every time
        // readline arms for input. Compared against command timing to tell
        // whether re-arming ran after the previous command completed.
        final AtomicLong lastReadlineArmNanos = new AtomicLong();
        // Connection-close timestamp (nanos, 0 = never). Any nonzero value
        // proves the shutdown path ran instead of readline re-arming.
        final AtomicLong connectionCloseNanos = new AtomicLong();
        // Handler-replacement counter for the re-arm tripwire (see
        // AeshLauncherImpl). Only ever increases.
        final AtomicLong armCount = new AtomicLong();

        public AeshTestThread(Runnable target, String name, ClassLoader contextClassLoader,
                InputStream testInput, OutputStream testOutput,
                LinkedBlockingQueue<Object> signalQueue,
                OutputStream commandOutputCapture,
                ConcurrentLinkedQueue<String> inputLineQueue) {
            super(target, name);
            setContextClassLoader(contextClassLoader);
            setDaemon(true);
            this.testInput = testInput;
            this.testOutput = testOutput;
            this.signalQueue = signalQueue;
            this.commandOutputCapture = commandOutputCapture;
            this.inputLineQueue = inputLineQueue;
        }

        /**
         * Returns the shared reader-death record. Used by the test side
         * (which holds the thread reference) to detect a dead session.
         */
        public AtomicReference<Throwable> readerDeath() {
            return readerDeath;
        }

        /**
         * Returns the shared readline-arm timestamp. Used by the test side
         * (which holds the thread reference) for hang diagnostics.
         */
        public AtomicLong lastReadlineArmNanos() {
            return lastReadlineArmNanos;
        }

        /**
         * Returns the shared connection-close timestamp. Used by the test
         * side (which holds the thread reference) for hang diagnostics.
         */
        public AtomicLong connectionCloseNanos() {
            return connectionCloseNanos;
        }

        /**
         * Returns the shared handler-replacement counter. Used by the test
         * side (which holds the thread reference) for the re-arm tripwire.
         */
        public AtomicLong armCount() {
            return armCount;
        }
    }
}
