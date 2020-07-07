package io.quarkus.jberet.runtime;

import java.util.concurrent.Executor;

import org.jberet.spi.JobExecutor;

class QuarkusJobExecutor extends JobExecutor {
    public QuarkusJobExecutor(Executor delegate) {
        super(delegate);
    }

    @Override
    protected int getMaximumPoolSize() {
        // From io.quarkus.smallrye.context.runtime.SmallRyeContextPropagationRecorder.initializeManagedExecutor and
        // io.smallrye.context.SmallRyeManagedExecutor.newThreadPoolExecutor
        // It is initialized with -1 and fallbacks to Runtime.getRuntime().availableProcessors();
        return Runtime.getRuntime().availableProcessors();
    }
}
