/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkus.runtime;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.jboss.logging.Logger;
import org.jboss.threads.EnhancedQueueExecutor;
import org.jboss.threads.JBossExecutors;
import org.jboss.threads.JBossThreadFactory;
import org.wildfly.common.cpu.ProcessorInfo;

import io.quarkus.runtime.annotations.Template;

/**
 *
 */
@Template
public class ExecutorTemplate {

    private static final Logger log = Logger.getLogger("io.quarkus.thread-pool");

    public ExecutorTemplate() {
    }

    /**
     * In dev mode for now we need the executor to last for the life of the app, as it is used by Undertow. This will likely
     * change
     */
    static CleanableExecutor devModeExecutor;

    public ExecutorService setupRunTime(ShutdownContext shutdownContext, ThreadPoolConfig threadPoolConfig,
            LaunchMode launchMode) {
        if (devModeExecutor != null) {
            return devModeExecutor;
        }
        final EnhancedQueueExecutor underlying = createExecutor(threadPoolConfig);
        ExecutorService executor;
        Runnable shutdownTask = createShutdownTask(threadPoolConfig, underlying);
        if (launchMode == LaunchMode.DEVELOPMENT) {
            devModeExecutor = new CleanableExecutor(underlying);
            shutdownContext.addShutdownTask(new Runnable() {
                @Override
                public void run() {
                    devModeExecutor.clean();
                }
            });
            executor = devModeExecutor;
            Runtime.getRuntime().addShutdownHook(new Thread(shutdownTask, "Executor shutdown thread"));
        } else {
            shutdownContext.addShutdownTask(shutdownTask);
            executor = underlying;
        }
        return executor;
    }

    public static ExecutorService createDevModeExecutorForFailedStart(ThreadPoolConfig config) {
        EnhancedQueueExecutor underlying = createExecutor(config);
        Runnable task = createShutdownTask(config, underlying);
        devModeExecutor = new CleanableExecutor(underlying);
        Runtime.getRuntime().addShutdownHook(new Thread(task, "Executor shutdown thread"));
        return devModeExecutor;
    }

    private static Runnable createShutdownTask(ThreadPoolConfig threadPoolConfig, EnhancedQueueExecutor executor) {
        return new Runnable() {
            @Override
            public void run() {
                executor.shutdown();
                final Duration shutdownTimeout = threadPoolConfig.shutdownTimeout;
                final Optional<Duration> optionalInterval = threadPoolConfig.shutdownCheckInterval;
                long remaining = shutdownTimeout.toNanos();
                final long interval = optionalInterval.orElse(Duration.ofNanos(Long.MAX_VALUE)).toNanos();
                long intervalRemaining = interval;
                long interruptRemaining = threadPoolConfig.shutdownInterrupt.toNanos();

                long start = System.nanoTime();
                for (;;)
                    try {
                        if (!executor.awaitTermination(Math.min(remaining, intervalRemaining), TimeUnit.MILLISECONDS)) {
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
                                            Integer.valueOf(runnables.size()), Integer.valueOf(executor.getActiveCount()));
                                } else {
                                    log.warnf("Thread pool shutdown failed: %d threads still running",
                                            Integer.valueOf(executor.getActiveCount()));
                                }
                                break;
                            }
                            if (intervalRemaining <= 0) {
                                intervalRemaining = interval;
                                // do some probing
                                final int queueSize = executor.getQueueSize();
                                final Thread[] runningThreads = executor.getRunningThreads();
                                log.infof("Awaiting thread pool shutdown; %d thread(s) running with %d task(s) waiting",
                                        Integer.valueOf(runningThreads.length), Integer.valueOf(queueSize));
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
                                if (realWaiting == 0 && queueSize == 0) {
                                    // just exit
                                    executor.shutdownNow();
                                    break;
                                }
                            }
                        }
                        return;
                    } catch (InterruptedException ignored) {
                    }
            }
        };
    }

    private static EnhancedQueueExecutor createExecutor(ThreadPoolConfig threadPoolConfig) {
        final JBossThreadFactory threadFactory = new JBossThreadFactory(new ThreadGroup("executor"), Boolean.TRUE, null,
                "executor-thread-%t", JBossExecutors.loggingExceptionHandler("org.jboss.executor.uncaught"), null);
        final EnhancedQueueExecutor.Builder builder = new EnhancedQueueExecutor.Builder()
                .setRegisterMBean(false)
                .setHandoffExecutor(JBossExecutors.rejectingExecutor())
                .setThreadFactory(JBossExecutors.resettingThreadFactory(threadFactory));
        final int cpus = ProcessorInfo.availableProcessors();
        // run time config variables
        builder.setCorePoolSize(threadPoolConfig.coreThreads);
        builder.setMaximumPoolSize(threadPoolConfig.maxThreads.orElse(8 * cpus));
        builder.setMaximumQueueSize(threadPoolConfig.queueSize);
        builder.setGrowthResistance(threadPoolConfig.growthResistance);
        builder.setKeepAliveTime(threadPoolConfig.keepAliveTime);
        return builder.build();
    }

}
