package io.quarkus.quartz.runtime;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;

import io.quarkus.scheduler.spi.JobInstrumenter;
import io.quarkus.scheduler.spi.JobInstrumenter.JobInstrumentationContext;

/**
 *
 * @see JobInstrumenter
 */
class InstrumentedJob implements Job {

    private final Job delegate;
    private final JobInstrumenter instrumenter;

    InstrumentedJob(Job delegate, JobInstrumenter instrumenter) {
        this.delegate = delegate;
        this.instrumenter = instrumenter;
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        instrumenter.instrument(new JobInstrumentationContext() {

            @Override
            public CompletionStage<Void> executeJob() {
                try {
                    delegate.execute(context);
                    return CompletableFuture.completedFuture(null);
                } catch (Exception e) {
                    return CompletableFuture.failedFuture(e);
                }

            }

            @Override
            public String getSpanName() {
                JobKey key = context.getJobDetail().getKey();
                return key.getGroup() + '.' + key.getName();
            }
        });
    }

}
