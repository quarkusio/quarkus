package io.quarkus.vertx.core.runtime;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.logging.Logger;
import org.jboss.threads.EnhancedQueueExecutor;
import org.jboss.threads.JBossExecutors;
import org.wildfly.common.cpu.ProcessorInfo;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.vertx.core.runtime.config.VertxConfiguration;
import io.vertx.core.spi.ExecutorServiceFactory;

public class QuarkusExecutorFactory implements ExecutorServiceFactory {
    static volatile ExecutorService sharedExecutor;
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
        if (executorCount.incrementAndGet() == 1) {
            if (launchMode != LaunchMode.DEVELOPMENT) {
                if (sharedExecutor == null) {
                    log.warn("Shared executor not set. Unshared executor will be created for blocking work");
                    // This should only happen in tests using Vertx directly in a unit test
                    sharedExecutor = internalCreateExecutor(threadFactory, concurrency, maxConcurrency);
                }
                return sharedExecutor;
            }
        }

        return internalCreateExecutor(threadFactory, concurrency, maxConcurrency);
    }

    private ExecutorService internalCreateExecutor(ThreadFactory threadFactory, Integer concurrency, Integer maxConcurrency) {
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

        final EnhancedQueueExecutor eqe = builder.build();

        if (conf != null && conf.prefill) {
            eqe.prestartAllCoreThreads();
        }

        return eqe;
    }
}
