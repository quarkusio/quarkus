package io.quarkus.runtime.execution;

import java.io.Serializable;

/**
 * The shutdown hook execution handler. This handler makes sure that calls to {@link System#exit} result in termination
 * of the application. Handlers before this handler may not be run on exit, so this handler should generally be the
 * first one in the chain.
 */
public final class ShutdownHookExecutionHandler implements ExecutionHandler, Serializable {
    /**
     * The single instance.
     */
    public static final ShutdownHookExecutionHandler INSTANCE = new ShutdownHookExecutionHandler();

    private ShutdownHookExecutionHandler() {
    }

    public int run(final ExecutionChain chain, final ExecutionContext context) throws Exception {
        ShutdownHookThread shutdownHookThread = new ShutdownHookThread();
        try {
            Runtime.getRuntime().addShutdownHook(shutdownHookThread);
        } catch (IllegalStateException ignored) {
            Execution.signalAsyncExit();
            throw new AsynchronousExitException();
        }
        try {
            return chain.proceed(context);
        } finally {
            try {
                Runtime.getRuntime().removeShutdownHook(shutdownHookThread);
            } catch (RuntimeException ignored) {
            }
        }
    }

    class ShutdownHookThread extends Thread {

        ShutdownHookThread() {
            super("Shutdown thread");
            setDaemon(false);
        }

        @Override
        public void run() {
            Execution.signalAsyncExitAndAwaitTerm();
            System.out.flush();
            System.err.flush();
        }

        @Override
        public String toString() {
            return getName();
        }
    }

    Object readResolve() {
        return INSTANCE;
    }

    Object writeReplace() {
        return INSTANCE;
    }
}
