package io.quarkus.jberet.runtime;

import java.util.concurrent.Executor;

import org.jberet.spi.JobExecutor;

class QuarkusJobExecutor extends JobExecutor {

    public QuarkusJobExecutor(Executor delegate) {
        super(delegate);
    }

    @Override
    protected int getMaximumPoolSize() {
        // TODO: this should come from the configuration
        return 10;
    }
}
