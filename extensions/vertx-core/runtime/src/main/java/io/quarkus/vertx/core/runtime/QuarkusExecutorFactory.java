package io.quarkus.vertx.core.runtime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.threads.EnhancedQueueExecutor;
import org.jboss.threads.JBossExecutors;
import org.wildfly.common.cpu.ProcessorInfo;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.vertx.core.runtime.config.VertxConfiguration;
import io.vertx.core.spi.ExecutorServiceFactory;

public class QuarkusExecutorFactory implements ExecutorServiceFactory {
    // TODO Figure out how to share this without breaking shutdown
    static volatile ExecutorService sharedExecutor;
    private static final AtomicInteger executorCount = new AtomicInteger(0);

    private final VertxConfiguration conf;
    private final LaunchMode launchMode;

    public QuarkusExecutorFactory(VertxConfiguration conf, LaunchMode launchMode) {
        this.conf = conf;
        this.launchMode = launchMode;
    }

    @Override
    public ExecutorService createExecutor(ThreadFactory threadFactory, Integer concurrency, Integer maxConcurrency) {
        if (executorCount.incrementAndGet() == 1) {
            if (sharedExecutor == null) {
                throw new IllegalStateException("Shared executor not set");
            }
            if (launchMode == LaunchMode.DEVELOPMENT) {
                //we use a proxy here, to avoid dev mode lifecycle issues
                //the underlying executor is replaced on restart
                //however even if the container is not up we can still execute tasks
                //this allows hot reload to work even when startup failed
                return new ExecutorService() {
                    @Override
                    public void shutdown() {
                        //ignore, lifecycle is managed elsewhere
                    }

                    @Override
                    public List<Runnable> shutdownNow() {
                        return Collections.emptyList();
                    }

                    @Override
                    public boolean isShutdown() {
                        return false;
                    }

                    @Override
                    public boolean isTerminated() {
                        return false;
                    }

                    @Override
                    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
                        return true;
                    }

                    @Override
                    public <T> Future<T> submit(Callable<T> task) {
                        ExecutorService exec = sharedExecutor;
                        if (exec != null) {
                            try {
                                return exec.submit(task);
                            } catch (RejectedExecutionException e) {
                                //ignore
                            }
                        }
                        CompletableFuture<T> ret = new CompletableFuture<>();
                        threadFactory.newThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    ret.complete(task.call());
                                } catch (Throwable t) {
                                    ret.completeExceptionally(t);
                                }
                            }
                        });
                        return ret;
                    }

                    @Override
                    public <T> Future<T> submit(Runnable task, T result) {
                        ExecutorService exec = sharedExecutor;
                        if (exec != null) {
                            try {
                                return exec.submit(task, result);
                            } catch (RejectedExecutionException e) {
                                //ignore
                            }
                        }
                        CompletableFuture<T> ret = new CompletableFuture<>();
                        threadFactory.newThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    task.run();
                                    ret.complete(result);
                                } catch (Throwable t) {
                                    ret.completeExceptionally(t);
                                }
                            }
                        });
                        return ret;
                    }

                    @Override
                    public Future<?> submit(Runnable task) {
                        ExecutorService exec = sharedExecutor;
                        if (exec != null) {
                            try {
                                return exec.submit(task);
                            } catch (RejectedExecutionException e) {
                                //ignore
                            }
                        }
                        CompletableFuture<?> ret = new CompletableFuture<>();
                        threadFactory.newThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    task.run();
                                    ret.complete(null);
                                } catch (Throwable t) {
                                    ret.completeExceptionally(t);
                                }
                            }
                        });
                        return ret;
                    }

                    @Override
                    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
                        List<Future<T>> ret = new ArrayList<>(tasks.size());
                        for (Callable<T> i : tasks) {
                            ret.add(submit(i));
                        }
                        return ret;
                    }

                    @Override
                    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
                            throws InterruptedException {
                        //TODO: this is technically wrong, but should never actually be called when the underlying executor is null
                        return sharedExecutor.invokeAll(tasks, timeout, unit);
                    }

                    @Override
                    public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
                            throws InterruptedException, ExecutionException {
                        //TODO: this is technically wrong, but should never actually be called when the underlying executor is null
                        return sharedExecutor.invokeAny(tasks);
                    }

                    @Override
                    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
                            throws InterruptedException, ExecutionException, TimeoutException {
                        return sharedExecutor.invokeAny(tasks, timeout, unit);
                    }

                    @Override
                    public void execute(Runnable command) {
                        submit(command);
                    }
                };
            } else {
                return sharedExecutor;
            }
        }
        final EnhancedQueueExecutor.Builder builder = new EnhancedQueueExecutor.Builder()
                .setRegisterMBean(false)
                .setHandoffExecutor(JBossExecutors.rejectingExecutor())
                .setThreadFactory(JBossExecutors.resettingThreadFactory(threadFactory));
        final int cpus = ProcessorInfo.availableProcessors();
        // run time config variables
        builder.setCorePoolSize(concurrency);
        builder.setMaximumPoolSize(maxConcurrency != null ? maxConcurrency : Math.max(8 * cpus, 200));

        if (conf != null) {
            if (conf.queueSize.isPresent()) {
                if (conf.queueSize.getAsInt() < 0) {
                    builder.setMaximumQueueSize(Integer.MAX_VALUE);
                } else {
                    builder.setMaximumQueueSize(conf.queueSize.getAsInt());
                }
            }
            builder.setGrowthResistance(conf.growthResistance);
            builder.setKeepAliveTime(conf.keepAliveTime);
        }

        return builder.build();
    }
}
