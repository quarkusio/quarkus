package io.quarkus.runtime;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.IntSupplier;

import org.jboss.logging.Logger;
import org.jboss.threads.ContextHandler;
import org.jboss.threads.EnhancedQueueExecutor;
import org.jboss.threads.JBossExecutors;
import org.jboss.threads.JBossThreadFactory;

import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.util.NoopShutdownScheduledExecutorService;
import io.smallrye.common.cpu.ProcessorInfo;

/**
 *
 */
@Recorder
public class ExecutorRecorder {
    private static final Logger log = Logger.getLogger("io.quarkus.thread-pool");

    private static volatile Executor current;

    private final RuntimeValue<ThreadPoolConfig> threadPoolConfig;

    public ExecutorRecorder(RuntimeValue<ThreadPoolConfig> threadPoolConfig) {
        this.threadPoolConfig = threadPoolConfig;
    }

    public ScheduledExecutorService setupRunTime(ShutdownContext shutdownContext,
            LaunchMode launchMode, ThreadFactory threadFactory, ContextHandler<Object> contextHandler) {
        final EnhancedQueueExecutor underlying = createExecutor(threadPoolConfig.getValue(), threadFactory, contextHandler);
        if (launchMode == LaunchMode.DEVELOPMENT) {
            shutdownContext.addLastShutdownTask(new Runnable() {
                @Override
                public void run() {
                    for (Runnable i : underlying.shutdownNow()) {
                        Thread thread = new Thread(i, "Shutdown task thread");
                        thread.setDaemon(true);
                        thread.start();
                    }
                    current = null;

                }
            });
        } else {
            Runnable shutdownTask = createShutdownTask(threadPoolConfig.getValue(), underlying);
            shutdownContext.addLastShutdownTask(shutdownTask);
        }
        if (threadPoolConfig.getValue().prefill()) {
            underlying.prestartAllCoreThreads();
        }
        ScheduledExecutorService managed = underlying;
        // In prod and test mode, we wrap the ExecutorService and the shutdown() and shutdownNow() are deliberately not delegated
        // This is to prevent the application and other extensions from shutting down the executor service
        // The problem was described in https://github.com/quarkusio/quarkus/issues/16833#issuecomment-1917042589
        // and https://github.com/quarkusio/quarkus/issues/43228
        // For example, the Vertx instance is closed before io.quarkus.runtime.ExecutorRecorder.createShutdownTask() is used
        // And when it's closed the underlying worker thread pool (which is in the prod mode backed by the ExecutorBuildItem) is closed as well
        // As a result the quarkus.thread-pool.shutdown-interrupt config property and logic defined in ExecutorRecorder.createShutdownTask() is completely ignored
        if (launchMode != LaunchMode.DEVELOPMENT) {
            managed = new NoopShutdownScheduledExecutorService(underlying);
        }
        current = managed;
        return managed;
    }

    private static Runnable createShutdownTask(ThreadPoolConfig threadPoolConfig, EnhancedQueueExecutor executor) {
        return new Runnable() {
            @Override
            public void run() {
                executor.shutdown();
                final Duration shutdownTimeout = threadPoolConfig.shutdownTimeout();
                final Optional<Duration> optionalInterval = threadPoolConfig.shutdownCheckInterval();
                long remaining = shutdownTimeout.toNanos();
                final long interval = optionalInterval.orElse(Duration.ofNanos(Long.MAX_VALUE)).toNanos();
                long intervalRemaining = interval;
                long interruptRemaining = threadPoolConfig.shutdownInterrupt().toNanos();

                long start = System.nanoTime();
                int loop = 1;
                for (;;) {
                    // This log can be very useful when debugging problems
                    log.debugf("loop: %s, remaining: %s, intervalRemaining: %s, interruptRemaining: %s", loop++, remaining,
                            intervalRemaining, interruptRemaining);
                    try {
                        if (!executor.awaitTermination(Math.min(remaining, intervalRemaining), TimeUnit.NANOSECONDS)) {
                            long elapsed = System.nanoTime() - start;
                            intervalRemaining -= elapsed;
                            remaining -= elapsed;
                            interruptRemaining -= elapsed;
                            if (interruptRemaining <= 0) {
                                executor.shutdown(true);
                            }
                            if (remaining <= 0) {
                                // done waiting
                                final List<Runnable> runnables = executor.shutdownNow();
                                if (!runnables.isEmpty()) {
                                    log.warnf("Thread pool shutdown failed: discarding %d tasks, %d threads still running",
                                            runnables.size(), executor.getActiveCount());
                                } else {
                                    log.warnf("Thread pool shutdown failed: %d threads still running",
                                            executor.getActiveCount());
                                }
                                break;
                            }
                            if (intervalRemaining <= 0) {
                                intervalRemaining = interval;
                                // do some probing
                                final int queueSize = executor.getQueueSize();
                                final Thread[] runningThreads = executor.getRunningThreads();
                                if (queueSize <= 0) {
                                    log.infof("Awaiting thread pool shutdown; %d thread(s) running",
                                            runningThreads.length);
                                } else {
                                    log.infof("Awaiting thread pool shutdown; %d thread(s) running with %d task(s) waiting",
                                            runningThreads.length, queueSize);
                                }
                                // make sure no threads are stuck in {@code exit()}
                                int realWaiting = runningThreads.length;
                                for (Thread thr : runningThreads) {
                                    final StackTraceElement[] stackTrace = thr.getStackTrace();
                                    for (int i = 0; i < stackTrace.length && i < 8; i++) {
                                        if (stackTrace[i].getClassName().equals("java.lang.System")
                                                && stackTrace[i].getMethodName().equals("exit")) {
                                            final Throwable t = new Throwable();
                                            t.setStackTrace(stackTrace);
                                            log.errorf(t, "Thread %s is blocked in System.exit(); pooled (Executor) threads "
                                                    + "should never call this method because it never returns, thus preventing "
                                                    + "the thread pool from shutting down in a timely manner.  This is the "
                                                    + "stack trace of the call", thr.getName());
                                            // don't bother waiting for exit() to return
                                            realWaiting--;
                                            break;
                                        }
                                    }
                                }
                                if (realWaiting == 0) {
                                    // just exit
                                    executor.shutdownNow();
                                    break;
                                }
                            }
                        } else {
                            return;
                        }
                    } catch (InterruptedException ignored) {
                    }
                }
            }
        };
    }

    private static EnhancedQueueExecutor createExecutor(ThreadPoolConfig threadPoolConfig, ThreadFactory threadFactory,
            ContextHandler<Object> contextHandler) {
        if (threadFactory == null) {
            threadFactory = new JBossThreadFactory(new ThreadGroup("executor"), Boolean.TRUE, null,
                    "executor-thread-%t", JBossExecutors.loggingExceptionHandler("org.jboss.executor.uncaught"), null);
        }
        final EnhancedQueueExecutor.Builder builder = new EnhancedQueueExecutor.Builder()
                .setRegisterMBean(false)
                .setHandoffExecutor(JBossExecutors.rejectingExecutor())
                .setThreadFactory(JBossExecutors.resettingThreadFactory(threadFactory));
        // run time config variables
        builder.setCorePoolSize(threadPoolConfig.coreThreads());
        builder.setMaximumPoolSize(getMaxSize(threadPoolConfig));
        if (threadPoolConfig.queueSize().isPresent()) {
            if (threadPoolConfig.queueSize().getAsInt() < 0) {
                builder.setMaximumQueueSize(Integer.MAX_VALUE);
                builder.setQueueLimited(false);
            } else {
                builder.setMaximumQueueSize(threadPoolConfig.queueSize().getAsInt());
            }
        } else {
            builder.setQueueLimited(false);
        }
        builder.setGrowthResistance(threadPoolConfig.growthResistance());
        builder.setKeepAliveTime(threadPoolConfig.keepAliveTime());

        if (contextHandler != null) {
            builder.setContextHandler(contextHandler);
        }

        return builder.build();
    }

    public static int getMaxSize(ThreadPoolConfig threadPoolConfig) {
        return threadPoolConfig.maxThreads().orElseGet(MaxThreadsCalculator.INSTANCE);
    }

    public static int calculateMaxThreads() {
        return MaxThreadsCalculator.INSTANCE.getAsInt();
    }

    /**
     * NOTE: This is not folded at native image build time, so it works as expected
     */
    private static final class MaxThreadsCalculator implements IntSupplier {

        private static final MaxThreadsCalculator INSTANCE = new MaxThreadsCalculator();

        private MaxThreadsCalculator() {
        }

        @Override
        public int getAsInt() {
            return Holder.CALCULATION;
        }

        private static class Holder {
            private static final int DEFAULT_MAX_THREADS = 200;
            private static final int CALCULATION = Math.max(8 * ProcessorInfo.availableProcessors(), DEFAULT_MAX_THREADS);
        }
    }

    public static Executor getCurrent() {
        return current;
    }
}
