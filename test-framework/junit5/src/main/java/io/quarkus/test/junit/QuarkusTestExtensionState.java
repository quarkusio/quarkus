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
    // TODO probably need to send this across too?
    private Thread shutdownHook;
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

            // TODO check the class, obviously
            // TODO find a clean mechanism for cloning subclasses that isn't hardcoding;
            if (state.getClass()
                    .getName()
                    .equals(QuarkusTestExtension.ExtensionState.class.getName())) {
                QuarkusTestExtensionState answer = new QuarkusTestExtension.ExtensionState(trm, resource, shutdownHook);
                return answer;
            } else {
                throw new RuntimeException("Not implemented - sort out state cloning!");
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

    public Thread shutdownHook() {
        return shutdownHook;
    }

    public QuarkusTestExtensionState(Closeable testResourceManager, Closeable resource) {
        this.testResourceManager = testResourceManager;
        this.resource = resource;

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

    public QuarkusTestExtensionState(Closeable testResourceManager, Closeable resource, Thread shutdownHook) {
        this.testResourceManager = testResourceManager;
        this.resource = resource;

        this.shutdownHook = shutdownHook;
    }

    public Throwable getTestErrorCause() {
        return testErrorCause;
    }

    @Override
    public void close() throws IOException {
        if (closed.compareAndSet(false, true)) {
            doClose();

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
