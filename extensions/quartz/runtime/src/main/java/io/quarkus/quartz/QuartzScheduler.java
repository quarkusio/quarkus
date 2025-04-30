package io.quarkus.quartz;

import io.quarkus.scheduler.Scheduler;

/**
 * Quartz-specific implementation of {@link Scheduler}.
 */
public interface QuartzScheduler extends Scheduler {

    /**
     *
     * @return the underlying {@link org.quartz.Scheduler} instance, or {@code null} if the scheduler was not started
     */
    org.quartz.Scheduler getScheduler();

    @Override
    QuartzJobDefinition newJob(String identity);

    interface QuartzJobDefinition extends JobDefinition<QuartzJobDefinition> {

        /**
         *
         * @return self
         * @see Nonconcurrent
         */
        QuartzJobDefinition setNonconcurrent();

    }

}
