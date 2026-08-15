package io.quarkus.aesh.runtime;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.LinkedBlockingQueue;

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

        public AeshTestThread(Runnable target, String name, ClassLoader contextClassLoader,
                InputStream testInput, OutputStream testOutput,
                LinkedBlockingQueue<Object> signalQueue) {
            super(target, name);
            setContextClassLoader(contextClassLoader);
            setDaemon(true);
            this.testInput = testInput;
            this.testOutput = testOutput;
            this.signalQueue = signalQueue;
        }
    }
}
