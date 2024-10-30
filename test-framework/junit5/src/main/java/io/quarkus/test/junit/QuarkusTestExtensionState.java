package io.quarkus.test.junit;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.extension.ExtensionContext;

import io.quarkus.test.common.TestResourceManager;

public class QuarkusTestExtensionState implements ExtensionContext.Store.CloseableResource {

    private final AtomicBoolean closed = new AtomicBoolean();

    protected Closeable testResourceManager;
    protected Closeable resource;
    private Thread shutdownHook;
    private Throwable testErrorCause;

    public QuarkusTestExtensionState(Closeable testResourceManager, Closeable resource) {
        this.testResourceManager = testResourceManager;
        this.resource = resource;
        this.shutdownHook = new ShutdownThread(this);
        Runtime.getRuntime().addShutdownHook(shutdownHook);
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
                shutdownHook = null;
                testResourceManager = null;
                resource = null;
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

    private static final class ShutdownThread extends Thread {

        public ShutdownThread(QuarkusTestExtensionState state) {
            super(new ShutdownHook(state), "Quarkus Test Cleanup Shutdown Thread");
        }
    }

    private static final class ShutdownHook implements Runnable {

        private QuarkusTestExtensionState state;

        private ShutdownHook(QuarkusTestExtensionState state) {
            this.state = state;
        }

        @Override
        public void run() {
            try {
                state.close();
            } catch (IOException ignored) {
            } finally {
                state = null;
            }
        }
    }
}
