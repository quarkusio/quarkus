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

    private final Instance.Handle<? extends Job> handle;
    private final Job beanInstance;

    public CdiAwareJob(Instance.Handle<? extends Job> handle) {
        this.handle = handle;
        this.beanInstance = handle.get();
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        boolean refire = false;
        try {
            beanInstance.execute(context);
        } catch (JobExecutionException e) {
            refire = e.refireImmediately();
            throw e;
        } finally {
            if (refire != true && handle.getBean().getScope().equals(Dependent.class)) {
                handle.destroy();
            }
        }
    }

    @Override
    public void interrupt() throws UnableToInterruptJobException {
        // delegate if possible; throw an exception in other cases
        if (InterruptableJob.class.isAssignableFrom(handle.getBean().getBeanClass())) {
            ((InterruptableJob) beanInstance).interrupt();
        } else {
            throw new UnableToInterruptJobException("Job " + handle.getBean().getBeanClass()
                    + " can not be interrupted, since it does not implement " + InterruptableJob.class.getName());
        }
    }
}
