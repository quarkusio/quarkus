package io.quarkus.virtual.threads;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class VirtualThreadsRecorder {
    private static final Logger logger = Logger.getLogger("io.quarkus.virtual-threads");

    /**
     * Should use the instance variable instead.
     */
    @Deprecated(since = "3.25", forRemoval = true)
    static volatile VirtualThreadsConfig config;
    private static volatile ExecutorService current;
    private static final Object lock = new Object();

    private final VirtualThreadsConfig runtimeConfig;

    public VirtualThreadsRecorder(final VirtualThreadsConfig runtimeConfig) {
        this.runtimeConfig = runtimeConfig;
        config = runtimeConfig;
    }

    public static Supplier<ExecutorService> VIRTUAL_THREADS_EXECUTOR_SUPPLIER = new Supplier<ExecutorService>() {
        @Override
        public ExecutorService get() {
            return new DelegatingExecutorService(VirtualThreadsRecorder.getCurrent());
        }
    };

    public void setupVirtualThreads(ShutdownContext shutdownContext, LaunchMode launchMode) {
        if (runtimeConfig.enabled()) {
            if (launchMode == LaunchMode.DEVELOPMENT) {
                shutdownContext.addLastShutdownTask(new Runnable() {
                    @Override
                    public void run() {
                        ExecutorService service = current;
                        if (service != null) {
                            service.shutdownNow();
                        }
                        current = null;
                    }
                });
            } else {
                shutdownContext.addLastShutdownTask(new Runnable() {
                    @Override
                    public void run() {
                        ExecutorService service = current;
                        current = null;
                        if (service != null) {
                            service.shutdown();

                            final long timeout = runtimeConfig.shutdownTimeout().toNanos();
                            final long interval = runtimeConfig.shutdownCheckInterval().orElse(
                                    runtimeConfig.shutdownTimeout()).toNanos();

                            long start = System.nanoTime();
                            int loop = 1;
                            long elapsed = 0;
                            for (;;) {
                                // This log can be very useful when debugging problems
                                logger.debugf("Await termination loop: %s, remaining: %s", loop++, timeout - elapsed);
                                try {
                                    if (!service.awaitTermination(Math.min(timeout, interval), NANOSECONDS)) {
                                        elapsed = System.nanoTime() - start;
                                        if (elapsed >= timeout) {
                                            service.shutdownNow();
                                            break;
                                        }
                                    } else {
                                        return;
                                    }
                                } catch (InterruptedException ignored) {
                                }
                            }
                        }
                    }
                });
            }
        }
    }

    public Supplier<ExecutorService> getCurrentSupplier() {
        return VIRTUAL_THREADS_EXECUTOR_SUPPLIER;
    }

    public static ExecutorService getCurrent() {
        ExecutorService executor = current;
        if (executor != null) {
            return executor;
        }
        synchronized (lock) {
            if (current == null) {
                current = createExecutor();
            }
            return current;
        }
    }

    static ExecutorService newVirtualThreadPerTaskExecutorWithName(String prefix) {
        Thread.Builder.OfVirtual builder = Thread.ofVirtual();
        if (prefix != null) {
            builder = builder.name(prefix, 0);
        }
        builder = builder.uncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                logger.errorf(e, "Thread %s threw an uncaught exception:", t);
            }
        });
        ThreadFactory tf = builder.factory();

        return Executors.newThreadPerTaskExecutor(tf);
    }

    private static ExecutorService createExecutor() {
        if (config.enabled()) {
            String prefix = config.namePrefix().orElse(null);
            return new ContextPreservingExecutorService(newVirtualThreadPerTaskExecutorWithName(prefix));
        }
        // Fallback to regular worker threads
        return new FallbackVirtualThreadsExecutorService();
    }
}
