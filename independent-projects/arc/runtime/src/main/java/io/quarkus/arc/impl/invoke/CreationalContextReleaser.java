package io.quarkus.arc.impl.invoke;

import jakarta.enterprise.context.spi.CreationalContext;

public final class CreationalContextReleaser {
    // we don't have to ensure that `CC.release()` is called exactly once, because
    // our implementation is idempotent (see `CreationalContextImpl.release()` and
    // `AbstractInstanceHandle.destroy()`)

    public static final class ForReturnType implements Runnable {
        private final CreationalContext<?> creationalContext;

        public ForReturnType(CreationalContext<?> creationalContext) {
            this.creationalContext = creationalContext;
        }

        @Override
        public void run() {
            creationalContext.release();
        }
    }

    public static final class ForParameterType implements Runnable {
        private final CreationalContext<?> creationalContext;
        private volatile boolean methodReturned = false;
        private volatile boolean runCalled = false;

        public ForParameterType(CreationalContext<?> creationalContext) {
            this.creationalContext = creationalContext;
        }

        public void methodReturned() {
            methodReturned = true;
            if (runCalled) {
                creationalContext.release();
            }
        }

        @Override
        public void run() {
            runCalled = true;
            if (methodReturned) {
                creationalContext.release();
            }
        }
    }
}
