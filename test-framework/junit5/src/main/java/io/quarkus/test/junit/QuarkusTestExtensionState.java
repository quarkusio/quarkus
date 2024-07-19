package io.quarkus.test.junit;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.extension.ExtensionContext;

import io.quarkus.test.common.TestResourceManager;

public class QuarkusTestExtensionState implements ExtensionContext.Store.CloseableResource {

    private final AtomicBoolean closed = new AtomicBoolean();

    protected final Closeable testResourceManager;
    protected final Closeable resource;
    private Thread shutdownHook;
    // TODO sort this out on the clone
    private final Runnable clearCallbacks;
    private Throwable testErrorCause;

    // NewSerializingDeepClone can't clone this
    public static QuarkusTestExtensionState clone(Object state) {
        System.out.println("HOLLY CLONEEEE" + state);
        try {
            Method trmm = state.getClass()
                    .getMethod("testResourceManager");
            Closeable trm = (Closeable) trmm.invoke(state);
            Method rmm = state.getClass()
                    .getMethod("resource");
            Closeable resource = (Closeable) rmm.invoke(state);

            Method shm = state.getClass()
                    .getMethod("shutdownHook");
            Thread shutdownHook = (Thread) shm.invoke(state);

            // TODO this is almost certainly wrong because we'll be getting a runner in the wrong classloader, but we need to think about what the right behaviour is here
            // Do nothing because the issue is fixed? Get the method name of the runner and bridge it across to this classloader?
            //  See https://github.com/quarkusio/quarkus/pull/44279#issuecomment-2548561806
            Method ccm = state.getClass()
                    .getMethod("clearCallbacksRunner");
            Runnable clearCallbacks = (Runnable) ccm.invoke(state);

            // TODO find a clean mechanism for cloning subclasses that isn't hardcoding; reflectively doing a class forName and assuming the correct constructor exists
            if (state.getClass()
                    .getName()
                    .equals(QuarkusTestExtension.ExtensionState.class.getName())) {
                QuarkusTestExtensionState answer = new QuarkusTestExtension.ExtensionState(trm, resource, clearCallbacks,
                        shutdownHook);
                return answer;
            } else if (state.getClass()
                    .getName()
                    .equals(QuarkusTestExtensionState.class.getName())) {
                QuarkusTestExtensionState answer = new QuarkusTestExtensionState(trm, resource, clearCallbacks, shutdownHook);
                return answer;
            } else {
                throw new RuntimeException("Not implemented. Cannot clone a state subclass of " + state.getClass());
            }

        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

    }

    // TODO #store

    // TODO can these be more private
    public Closeable testResourceManager() {
        return testResourceManager;
    }

    public Closeable resource() {
        return resource;
    }

    // Used reflectively
    public Thread shutdownHook() {
        return shutdownHook;
    }

    // Used reflectively
    public Runnable clearCallbacksRunner() {
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
