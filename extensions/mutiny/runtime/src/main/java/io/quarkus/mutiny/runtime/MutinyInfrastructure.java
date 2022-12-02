package io.quarkus.mutiny.runtime;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import org.jboss.logging.Logger;
import org.jboss.threads.ContextHandler;

import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.infrastructure.MutinyScheduler;

@Recorder
public class MutinyInfrastructure {

    public static final String VERTX_EVENT_LOOP_THREAD_PREFIX = "vert.x-eventloop-thread-";

    public void configureMutinyInfrastructure(ExecutorService executor, ShutdownContext shutdownContext,
            ContextHandler<Object> contextHandler) {
        // Mutiny leaks a ScheduledExecutorService if we don't do this
        Infrastructure.getDefaultWorkerPool().shutdown();

        // Since executor is not a ScheduledExecutorService and Mutiny needs one for scheduling we have to adapt one around the provided executor
        MutinyScheduler mutinyScheduler = new MutinyScheduler(executor) {
            @Override
            protected <V> RunnableScheduledFuture<V> decorateTask(Runnable runnable, RunnableScheduledFuture<V> task) {
                Object context = (contextHandler != null) ? contextHandler.captureContext() : null;
                return super.decorateTask(runnable, new ContextualRunnableScheduledFuture<>(contextHandler, context, task));
            }

            @Override
            protected <V> RunnableScheduledFuture<V> decorateTask(Callable<V> callable, RunnableScheduledFuture<V> task) {
                Object context = (contextHandler != null) ? contextHandler.captureContext() : null;
                return super.decorateTask(callable, new ContextualRunnableScheduledFuture<>(contextHandler, context, task));
            }
        };
        Infrastructure.setDefaultExecutor(new ScheduledExecutorService() {

            @Override
            public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
                return mutinyScheduler.schedule(command, delay, unit);
            }

            @Override
            public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
                return mutinyScheduler.schedule(callable, delay, unit);
            }

            @Override
            public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
                return mutinyScheduler.scheduleAtFixedRate(command, initialDelay, period, unit);
            }

            @Override
            public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
                return mutinyScheduler.scheduleWithFixedDelay(command, initialDelay, delay, unit);
            }

            @Override
            public void shutdown() {
                mutinyScheduler.shutdown(); // ...but do not shut `executor` down
            }

            @Override
            public List<Runnable> shutdownNow() {
                return mutinyScheduler.shutdownNow();
            }

            @Override
            public boolean isShutdown() {
                return mutinyScheduler.isShutdown();
            }

            @Override
            public boolean isTerminated() {
                return mutinyScheduler.isTerminated();
            }

            @Override
            public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
                return mutinyScheduler.awaitTermination(timeout, unit);
            }

            @Override
            public <T> Future<T> submit(Callable<T> task) {
                return executor.submit(task);
            }

            @Override
            public <T> Future<T> submit(Runnable task, T result) {
                return executor.submit(task, result);
            }

            @Override
            public Future<?> submit(Runnable task) {
                return executor.submit(task);
            }

            @Override
            public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
                return executor.invokeAll(tasks);
            }

            @Override
            public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
                    throws InterruptedException {
                return executor.invokeAll(tasks, timeout, unit);
            }

            @Override
            public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
                return executor.invokeAny(tasks);
            }

            @Override
            public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
                    throws InterruptedException, ExecutionException, TimeoutException {
                return executor.invokeAny(tasks, timeout, unit);
            }

            @Override
            public void execute(Runnable command) {
                try {
                    executor.execute(command);
                } catch (RejectedExecutionException rejected) {
                    // Ignore submission failures on application shutdown
                    if (!executor.isShutdown() && !executor.isTerminated()) {
                        throw rejected;
                    }
                }
            }
        });

        shutdownContext.addLastShutdownTask(new Runnable() {
            @Override
            public void run() {
                mutinyScheduler.shutdown();
            }
        });
    }

    public void configureDroppedExceptionHandler() {
        Logger logger = Logger.getLogger(MutinyInfrastructure.class);
        Infrastructure.setDroppedExceptionHandler(new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) {
                logger.error("Mutiny had to drop the following exception", throwable);
            }
        });
    }

    public void configureThreadBlockingChecker() {
        Infrastructure.setCanCallerThreadBeBlockedSupplier(new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() {
                /*
                 * So far all threads and Vert.x worker threads can block, but Vert.x event-loop threads must not block.
                 * It is safe to detect Vert.x event-loop threads by naming convention.
                 *
                 * It also avoids adding a dependency of this extension on the Vert.x APIs to check if we are
                 * calling from a Vert.x event-loop context / thread.
                 */
                String threadName = Thread.currentThread().getName();
                return !threadName.startsWith(VERTX_EVENT_LOOP_THREAD_PREFIX);
            }
        });
    }

    public void configureOperatorLogger() {
        Logger logger = Logger.getLogger(MutinyInfrastructure.class);
        Infrastructure.setOperatorLogger(new Infrastructure.OperatorLogger() {
            @Override
            public void log(String identifier, String event, Object value, Throwable failure) {
                String log = identifier + " | ";
                if (failure != null) {
                    log = log + event + "(" + failure.getClass() + "(" + failure.getMessage() + "))";
                } else if (value != null) {
                    log = log + event + "(" + value + ")";
                } else {
                    log = log + event + "()";
                }
                logger.info(log);
            }
        });
    }
}
