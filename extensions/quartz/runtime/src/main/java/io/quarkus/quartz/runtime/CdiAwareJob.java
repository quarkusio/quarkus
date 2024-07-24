package io.quarkus.quartz.runtime;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;

import org.quartz.InterruptableJob;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.UnableToInterruptJobException;
import org.quartz.spi.TriggerFiredBundle;

/**
 * An abstraction allowing proper destruction of Job instances in case they are dependent beans.
 * According to {@link org.quartz.spi.JobFactory#newJob(TriggerFiredBundle, Scheduler)}, a new job instance is created for every
 * trigger.
 * We will therefore create a new dependent bean for every trigger and destroy it afterwards.
 */
class CdiAwareJob implements InterruptableJob {

    private final Instance<? extends Job> jobInstance;

    public CdiAwareJob(Instance<? extends Job> jobInstance) {
        this.jobInstance = jobInstance;
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        Instance.Handle<? extends Job> handle = jobInstance.getHandle();
        try {
            handle.get().execute(context);
        } finally {
            if (handle.getBean().getScope().equals(Dependent.class)) {
                handle.destroy();
            }
        }
    }

    @Override
    public void interrupt() throws UnableToInterruptJobException {
        Instance.Handle<? extends Job> handle = jobInstance.getHandle();
        // delegate if possible; throw an exception in other cases
        if (InterruptableJob.class.isAssignableFrom(handle.getBean().getBeanClass())) {
            try {
                ((InterruptableJob) handle.get()).interrupt();
            } finally {
                if (handle.getBean().getScope().equals(Dependent.class)) {
                    handle.destroy();
                }
            }
        } else {
            throw new UnableToInterruptJobException("Job " + handle.getBean().getBeanClass()
                    + " can not be interrupted, since it does not implement " + InterruptableJob.class.getName());
        }
    }
}
