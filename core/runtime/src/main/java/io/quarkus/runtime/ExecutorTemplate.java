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

import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
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
    static final Config config = ConfigProvider.getConfig();
    public static final String CORE_POOL_SIZE = "executor.core-pool-size";
    public static final String MAX_POOL_SIZE = "executor.max-pool-size";
    public static final String QUEUE_SIZE = "executor.queue-size";
    public static final String GROWTH_RESISTANCE = "executor.growth-resistance";
    public static final String KEEP_ALIVE_MILLIS = "executor.keep-alive-millis";

    public ExecutorTemplate() {
    }

    public Executor setupRunTime(ShutdownContext shutdownContext, int defaultCoreSize, int defaultMaxSize, int defaultQueueSize,
            float defaultGrowthResistance, int defaultKeepAliveMillis) {
        final JBossThreadFactory threadFactory = new JBossThreadFactory(new ThreadGroup("executor"), Boolean.TRUE, null,
                "executor-thread-%t", JBossExecutors.loggingExceptionHandler("org.jboss.executor.uncaught"), null);
        final EnhancedQueueExecutor.Builder builder = new EnhancedQueueExecutor.Builder()
                .setRegisterMBean(false)
                .setHandoffExecutor(JBossExecutors.rejectingExecutor())
                .setThreadFactory(JBossExecutors.resettingThreadFactory(threadFactory));
        final int cpus = ProcessorInfo.availableProcessors();
        // run time config variables
        builder.setCorePoolSize(getIntConfigVal(CORE_POOL_SIZE, defaultCoreSize == -1 ? 4 * cpus : defaultCoreSize));
        builder.setMaximumPoolSize(getIntConfigVal(MAX_POOL_SIZE, defaultMaxSize == -1 ? 10 * cpus : defaultMaxSize));
        builder.setMaximumQueueSize(getIntConfigVal(QUEUE_SIZE, defaultQueueSize));
        builder.setGrowthResistance(getFloatConfigVal(GROWTH_RESISTANCE, defaultGrowthResistance));
        builder.setKeepAliveTime(getIntConfigVal("executor.keep-alive-millis", defaultKeepAliveMillis), TimeUnit.MILLISECONDS);
        final EnhancedQueueExecutor executor = builder.build();
        shutdownContext.addShutdownTask(new Runnable() {
            @Override
            public void run() {
                executor.shutdown();
                for (;;)
                    try {
                        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.HOURS);
                        return;
                    } catch (InterruptedException ignored) {
                    }
            }
        });
        return executor;
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
