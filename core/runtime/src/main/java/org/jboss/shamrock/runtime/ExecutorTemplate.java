package org.jboss.shamrock.runtime;

import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.threads.EnhancedQueueExecutor;
import org.jboss.threads.JBossExecutors;
import org.jboss.threads.JBossThreadFactory;
import org.wildfly.common.cpu.ProcessorInfo;

/**
 *
 */
@Template
public class ExecutorTemplate {
    static final Config config = ConfigProvider.getConfig();
    public static final String CORE_POOL_SIZE = "executor.core-pool-size";
    public static final String MAX_POOL_SIZE = "executor.max-pool-size";
    public static final String QUEUE_SIZE = "executor.queue-size";
    public static final String GROWTH_RESISTANCE = "executor.growth-resistance";
    public static final String KEEP_ALIVE_MILLIS = "executor.keep-alive-millis";

    public ExecutorTemplate() {
    }

    public Executor setupRunTime(int defaultCoreSize, int defaultMaxSize, int defaultQueueSize, float defaultGrowthResistance, int defaultKeepAliveMillis) {
        final JBossThreadFactory threadFactory = new JBossThreadFactory(new ThreadGroup("executor"), Boolean.TRUE, null, "executor-thread-%t", JBossExecutors.loggingExceptionHandler("org.jboss.executor.uncaught"), null);
        final EnhancedQueueExecutor.Builder builder = new EnhancedQueueExecutor.Builder()
            .setRegisterMBean(false)
            .setHandoffExecutor(JBossExecutors.rejectingExecutor())
            .setThreadFactory(JBossExecutors.resettingThreadFactory(threadFactory));
        final int cpus = ProcessorInfo.availableProcessors();
        // run time config variables
        builder.setCorePoolSize(getIntConfigVal(CORE_POOL_SIZE, defaultCoreSize == - 1 ? 4 * cpus : defaultCoreSize));
        builder.setMaximumPoolSize(getIntConfigVal(MAX_POOL_SIZE, defaultMaxSize == - 1 ? 10 * cpus : defaultMaxSize));
        builder.setMaximumQueueSize(getIntConfigVal(QUEUE_SIZE, defaultQueueSize));
        builder.setGrowthResistance(getFloatConfigVal(GROWTH_RESISTANCE, defaultGrowthResistance));
        builder.setKeepAliveTime(getIntConfigVal("executor.keep-alive-millis", defaultKeepAliveMillis), TimeUnit.MILLISECONDS);
        return builder.build();
    }

    public static float getFloatConfigVal(final String key, final float defVal) {
        final Optional<Float> val = config.getOptionalValue(key, Float.class);
        return val.isPresent() ? val.get().floatValue() : defVal;
    }

    public static int getIntConfigVal(String key, int defVal) {
        final Optional<Integer> val = config.getOptionalValue(key, Integer.class);
        return val.isPresent() ? val.get().intValue() : defVal;
    }
}
