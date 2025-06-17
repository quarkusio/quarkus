package io.quarkus.test.junit;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

import io.quarkus.test.common.TestResourceManager;

public class QuarkusTestExtensionState implements AutoCloseable {

    private final AtomicBoolean closed = new AtomicBoolean();

    protected final Closeable testResourceManager;
    protected final Closeable resource;
    private Thread shutdownHook;
    private final Runnable clearCallbacks;
    private Throwable testErrorCause;

    // We need to move this between classloaders, and NewSerializingDeepClone can't clone this
    // Instead, clone by brute force and knowledge of internals
    public static QuarkusTestExtensionState clone(Object state) {
        try {
            Class<?> clazz = state.getClass();
            Method trmm = clazz.getMethod("getTestResourceManager");
            Closeable trm = (Closeable) trmm.invoke(state);

            Method rmm = clazz.getMethod("getResource");
            Closeable resource = (Closeable) rmm.invoke(state);

            Method shm = clazz.getMethod("getShutdownHook");
            Thread shutdownHook = (Thread) shm.invoke(state);

            Method ccm = clazz.getMethod("getClearCallbacksRunner");
            Runnable clearCallbacks = (Runnable) ccm.invoke(state);

            // This is a bit icky; we could avoid the hardcoding if we would reflectively do a class forName and assume the correct constructor exists, but I'm not sure that's much better
            if (clazz
                    .getName()
                    .equals(QuarkusTestExtension.ExtensionState.class.getName())) {
                QuarkusTestExtensionState answer = new QuarkusTestExtension.ExtensionState(trm, resource, clearCallbacks,
                        shutdownHook);
                return answer;
            } else if (clazz
                    .getName()
                    .equals(QuarkusTestExtensionState.class.getName())) {
                QuarkusTestExtensionState answer = new QuarkusTestExtensionState(trm, resource, clearCallbacks, shutdownHook);
                return answer;
            } else {
                throw new UnsupportedOperationException(
                        "Not implemented. Cannot clone a state subclass of " + clazz);
            }

        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

    }

    // Used reflectively
    public Closeable getTestResourceManager() {
        return testResourceManager;
    }

    // Used reflectively
    public Closeable getResource() {
        return resource;
    }

    // Used reflectively
    public Thread getShutdownHook() {
        return shutdownHook;
    }

    // Used reflectively
    public Runnable getClearCallbacksRunner() {
        return clearCallbacks;
    }

    public QuarkusTestExtensionState(Closeable testResourceManager, Closeable resource, Runnable clearCallbacks) {
        this.testResourceManager = testResourceManager;
        this.resource = resource;
        this.clearCallbacks = clearCallbacks;
        this.shutdownHook = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    QuarkusTestExtensionState.this.close();
                } catch (IOException ignored) {
                }
            }
        }, "Quarkus Test Cleanup Shutdown task");
        Runtime.getRuntime()
                .addShutdownHook(shutdownHook);
    }

    public QuarkusTestExtensionState(Closeable testResourceManager, Closeable resource, Runnable clearCallbacks,
            Thread shutdownHook) {
        this.testResourceManager = testResourceManager;
        this.resource = resource;
        this.clearCallbacks = clearCallbacks;

        this.shutdownHook = shutdownHook;
    }

    public Throwable getTestErrorCause() {
        return testErrorCause;
    }

    @Override
    public void close() throws IOException {
        if (closed.compareAndSet(false, true)) {
            doClose();
            clearCallbacks.run();

            try {
                Runtime.getRuntime().removeShutdownHook(shutdownHook);
            } catch (Throwable t) {
                //won't work if we are already shutting down
            } finally {
                // To make sure it doesn't get cloned
                shutdownHook = null;
            }
        }
    }

    protected void setTestFailed(Throwable failure) {
        try {
            this.testErrorCause = failure;
            if (testResourceManager instanceof TestResourceManager) {
                ((TestResourceManager) testResourceManager).setTestErrorCause(testErrorCause);
            } else {
                testResourceManager.getClass().getClassLoader().loadClass(TestResourceManager.class.getName())
                        .getMethod("setTestErrorCause", Throwable.class)
                        .invoke(testResourceManager, testErrorCause);

            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected void doClose() throws IOException {

    }
}
