package io.quarkus.quartz.runtime;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.spi.TriggerFiredBundle;

/**
 * An abstraction allowing proper destruction of Job instances in case they are dependent beans.
 * According to {@link org.quartz.spi.JobFactory#newJob(TriggerFiredBundle, Scheduler)}, a new job instance is created for every
 * trigger.
 * We will therefore create a new dependent bean for every trigger and destroy it afterwards.
 */
class CdiAwareJob implements Job {

    private final Instance.Handle<? extends Job> jobInstanceHandle;

    public CdiAwareJob(Instance.Handle<? extends Job> jobInstanceHandle) {
        this.jobInstanceHandle = jobInstanceHandle;
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            jobInstanceHandle.get().execute(context);
        } finally {
            if (jobInstanceHandle.getBean().getScope().equals(Dependent.class)) {
                jobInstanceHandle.destroy();
            }
        }
    }
}
