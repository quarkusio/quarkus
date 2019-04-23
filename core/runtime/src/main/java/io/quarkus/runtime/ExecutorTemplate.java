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

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

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

    public ExecutorTemplate() {
    }

    /**
     * In dev mode for now we need the executor to last for the life of the app, as it is used by Undertow. This will likely
     * change
     */
    static ExecutorService devModeExecutor;

    public ExecutorService setupRunTime(ShutdownContext shutdownContext, ThreadPoolConfig threadPoolConfig,
            LaunchMode launchMode) {
        if (devModeExecutor != null) {
            return devModeExecutor;
        }
        final JBossThreadFactory threadFactory = new JBossThreadFactory(new ThreadGroup("executor"), Boolean.TRUE, null,
                "executor-thread-%t", JBossExecutors.loggingExceptionHandler("org.jboss.executor.uncaught"), null);
        final EnhancedQueueExecutor.Builder builder = new EnhancedQueueExecutor.Builder()
                .setRegisterMBean(false)
                .setHandoffExecutor(JBossExecutors.rejectingExecutor())
                .setThreadFactory(JBossExecutors.resettingThreadFactory(threadFactory));
        final int cpus = ProcessorInfo.availableProcessors();
        // run time config variables
        builder.setCorePoolSize(threadPoolConfig.coreThreads);
        builder.setMaximumPoolSize(threadPoolConfig.maxThreads <= 0 ? 8 * cpus : threadPoolConfig.maxThreads);
        builder.setMaximumQueueSize(threadPoolConfig.queueSize);
        builder.setGrowthResistance(threadPoolConfig.growthResistance);
        builder.setKeepAliveTime(threadPoolConfig.keepAliveTime);
        final EnhancedQueueExecutor executor = builder.build();

        Runnable shutdownTask = new Runnable() {
            @Override
            public void run() {
                executor.shutdown();
                for (;;)
                    try {
                        if (!executor.awaitTermination(threadPoolConfig.shutdownTimeout.toMillis(), TimeUnit.MILLISECONDS)) {
                            List<Runnable> tasks = executor.shutdownNow();
                            for (Runnable i : tasks) {
                                //for any pending tasks we just spawn threads to run them on a best effort basis
                                //these threads are daemon threads so if everything shuts down they will not keep the
                                //JVM alive
                                Thread t = new Thread(i, "Shutdown thread");
                                t.setDaemon(true);
                                t.start();
                            }
                        }
                        return;
                    } catch (InterruptedException ignored) {
                    }
            }
        };
        if (launchMode == LaunchMode.DEVELOPMENT) {
            devModeExecutor = executor;
            Runtime.getRuntime().addShutdownHook(new Thread(shutdownTask, "Executor shutdown thread"));
        } else {
            shutdownContext.addShutdownTask(shutdownTask);
        }
        return executor;
    }
}
