package io.quarkus.test.junit;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.extension.ExtensionContext;

import io.quarkus.test.common.TestResourceManager;

public class QuarkusTestExtensionState implements ExtensionContext.Store.CloseableResource {

    private final AtomicBoolean closed = new AtomicBoolean();

    protected final Closeable testResourceManager;
    protected final Closeable resource;
    private final Thread shutdownHook;
    private final Runnable clearCallbacks;
    private Throwable testErrorCause;

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
        Runtime.getRuntime().addShutdownHook(shutdownHook);
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
