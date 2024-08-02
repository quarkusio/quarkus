package io.quarkus.scheduler.common.runtime;

import java.util.concurrent.CompletionStage;

import io.quarkus.scheduler.ScheduledExecution;
import io.quarkus.scheduler.spi.JobInstrumenter;
import io.quarkus.scheduler.spi.JobInstrumenter.JobInstrumentationContext;

/**
 *
 * @see JobInstrumenter
 */
public class InstrumentedInvoker extends DelegateInvoker {

    private final JobInstrumenter instrumenter;

    public InstrumentedInvoker(ScheduledInvoker delegate, JobInstrumenter instrumenter) {
        super(delegate);
        this.instrumenter = instrumenter;
    }

    @Override
    public CompletionStage<Void> invoke(ScheduledExecution execution) throws Exception {
        return instrumenter.instrument(new JobInstrumentationContext() {

            @Override
            public CompletionStage<Void> executeJob() {
                return invokeDelegate(execution);
            }

            @Override
            public String getSpanName() {
                return execution.getTrigger().getId();
            }
        });
    }

}
