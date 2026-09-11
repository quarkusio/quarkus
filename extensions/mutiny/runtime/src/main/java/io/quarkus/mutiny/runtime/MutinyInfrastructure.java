package io.quarkus.mutiny.runtime;

import java.util.concurrent.ScheduledExecutorService;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import org.jboss.logging.Logger;

import io.smallrye.mutiny.infrastructure.Infrastructure;

/**
 * Utility methods for configuring the Mutiny {@link Infrastructure} during application startup.
 */
public class MutinyInfrastructure {

    /**
     * The prefix used by Vert.x event loop thread names.
     */
    public static final String VERTX_EVENT_LOOP_THREAD_PREFIX = "vert.x-eventloop-thread-";

    /**
     * Configure the Mutiny infrastructure to use the given executor as the default executor.
     * The default worker pool is shut down first to prevent a leak.
     *
     * @param executor the executor to use as the default executor (must not be {@code null})
     */
    public static void configureMutinyInfrastructure(ScheduledExecutorService executor) {
        // Mutiny leaks a ScheduledExecutorService if we don't do this
        Infrastructure.getDefaultWorkerPool().shutdown();
        Infrastructure.setDefaultExecutor(executor);
    }

    /**
     * Configure the dropped exception handler to log exceptions that Mutiny could not deliver.
     */
    public static void configureDroppedExceptionHandler() {
        Logger logger = Logger.getLogger(MutinyInfrastructure.class);
        Infrastructure.setDroppedExceptionHandler(new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) {
                logger.error("Mutiny had to drop the following exception", throwable);
            }
        });
    }

    /**
     * Configure the thread blocking checker to prevent blocking on Vert.x event loop threads.
     */
    public static void configureThreadBlockingChecker() {
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

    /**
     * Configure the operator logger for Mutiny pipeline debugging.
     */
    public static void configureOperatorLogger() {
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
