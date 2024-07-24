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

    private final Instance<? extends Job> instance;
    // keep a contextual reference so that we avoid race condition where interrupt() is invoked after execute() finished
    private Job contextualReference;
    private Instance.Handle<? extends Job> handle;

    public CdiAwareJob(Instance<? extends Job> instance) {
        this.instance = instance;
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        if (handle == null) {
            this.handle = instance.getHandle();
            this.contextualReference = handle.get();
        }
        try {
            contextualReference.execute(context);
        } finally {
            if (handle.getBean().getScope().equals(Dependent.class)) {
                handle.destroy();
                // needed for jobs that can re-fire; we always want a new dependent bean there
                // intentionally not cleaning up bean reference as interrupt() can occur after execute()
                handle = null;
            }
        }
    }

    @Override
    public void interrupt() throws UnableToInterruptJobException {
        // delegate if possible; throw an exception in other cases
        if (InterruptableJob.class.isAssignableFrom(handle.getBean().getBeanClass())) {
            ((InterruptableJob) contextualReference).interrupt();
        } else {
            throw new UnableToInterruptJobException("Job " + handle.getBean().getBeanClass()
                    + " can not be interrupted, since it does not implement " + InterruptableJob.class.getName());
        }
    }
}
