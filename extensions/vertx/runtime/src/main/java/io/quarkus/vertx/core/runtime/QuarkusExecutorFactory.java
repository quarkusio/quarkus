package io.quarkus.vertx.core.runtime;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.jboss.logging.Logger;
import org.jboss.threads.EnhancedQueueExecutor;
import org.jboss.threads.JBossExecutors;

import io.quarkus.runtime.ExecutorRecorder;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.vertx.core.runtime.config.VertxConfiguration;
import io.vertx.core.spi.ExecutorServiceFactory;

public class QuarkusExecutorFactory implements ExecutorServiceFactory {
    static volatile ExecutorService sharedExecutor;
    static volatile DevModeExecutorService devModeExecutor;
    private static final AtomicInteger executorCount = new AtomicInteger(0);
    private static final Logger log = Logger.getLogger(QuarkusExecutorFactory.class);

    private final VertxConfiguration conf;
    private final LaunchMode launchMode;

    public QuarkusExecutorFactory(VertxConfiguration conf, LaunchMode launchMode) {
        this.conf = conf;
        this.launchMode = launchMode;
    }

    @Override
    public ExecutorService createExecutor(ThreadFactory threadFactory, Integer concurrency, Integer maxConcurrency) {
        // The current Vertx impl creates two external executors during initialization
        // The first one is used for the worker thread pool, the second one is used internally,
        // and additional executors may be created on demand
        // Unfortunately, there is no way to distinguish the particular executor types
        // Therefore, we only consider the first one as the worker thread pool
        // Note that in future versions of Vertx this may change!
        if (executorCount.incrementAndGet() == 1) {
            // The first executor should be the worker thread pool
            if (launchMode != LaunchMode.DEVELOPMENT) {
                if (sharedExecutor == null) {
                    log.warn("Shared executor not set. Unshared executor will be created for blocking work");
                    // This should only happen in tests using Vertx directly in a unit test
                    sharedExecutor = internalCreateExecutor(threadFactory, concurrency, maxConcurrency);
                }
                return sharedExecutor;
            } else {
                // In dev mode we use a special executor for the worker pool
                // where the underlying executor can be shut down and then replaced with a new re-initialized executor
                // This is a workaround to solve the problem described in https://github.com/quarkusio/quarkus/issues/16833#issuecomment-1917042589
                // The Vertx instance is reused between restarts but we must attempt to shut down this executor,
                // for example to cancel/interrupt the scheduled methods
                devModeExecutor = new DevModeExecutorService(new Supplier<ExecutorService>() {
                    @Override
                    public ExecutorService get() {
                        return internalCreateExecutor(threadFactory, concurrency, maxConcurrency);
                    }
                });
                return devModeExecutor;
            }
        }
        return internalCreateExecutor(threadFactory, concurrency, maxConcurrency);
    }

    /**
     * In dev mode, shut down the underlying executor and then initialize a new one.
     *
     * @see DevModeExecutorService
     */
    public static void reinitializeDevModeExecutor() {
        DevModeExecutorService executor = QuarkusExecutorFactory.devModeExecutor;
        if (executor != null) {
            executor.reinit();
        }
    }

    private ExecutorService internalCreateExecutor(ThreadFactory threadFactory, Integer concurrency, Integer maxConcurrency) {
        final EnhancedQueueExecutor.Builder builder = new EnhancedQueueExecutor.Builder()
                .setRegisterMBean(false)
                .setHandoffExecutor(JBossExecutors.rejectingExecutor())
                .setThreadFactory(JBossExecutors.resettingThreadFactory(threadFactory));
        // run time config variables
        builder.setCorePoolSize(concurrency);
        builder.setMaximumPoolSize(maxConcurrency != null ? maxConcurrency : ExecutorRecorder.calculateMaxThreads());

        if (conf != null) {
            if (conf.queueSize().isPresent()) {
                if (conf.queueSize().getAsInt() < 0) {
                    builder.setMaximumQueueSize(Integer.MAX_VALUE);
                } else {
                    builder.setMaximumQueueSize(conf.queueSize().getAsInt());
                }
            }
            builder.setGrowthResistance(conf.growthResistance());
            builder.setKeepAliveTime(conf.keepAliveTime());
        }

        final EnhancedQueueExecutor eqe = builder.build();

        if (conf != null && conf.prefill()) {
            eqe.prestartAllCoreThreads();
        }

        return eqe;
    }
}
