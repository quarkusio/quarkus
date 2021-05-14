package io.quarkus.vertx.core.runtime;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.threads.EnhancedQueueExecutor;
import org.jboss.threads.JBossExecutors;
import org.wildfly.common.cpu.ProcessorInfo;

import io.quarkus.vertx.core.runtime.config.VertxConfiguration;
import io.vertx.core.spi.ExecutorServiceFactory;

public class QuarkusExecutorFactory implements ExecutorServiceFactory {
    // TODO Figure out how to share this without breaking shutdown
    static ExecutorService sharedExecutor;
    private static final AtomicInteger executorCount = new AtomicInteger(0);

    private final VertxConfiguration conf;

    public QuarkusExecutorFactory(VertxConfiguration conf) {
        this.conf = conf;
    }

    @Override
    public ExecutorService createExecutor(ThreadFactory threadFactory, Integer concurrency, Integer maxConcurrency) {
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
