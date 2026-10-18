package io.quarkus.kubernetes.deployment;

import java.util.Optional;

import io.dekorate.kubernetes.annotation.CronJobConcurrencyPolicy;
import io.smallrye.config.WithDefault;

public interface CronJobConfig extends JobConfig {
    /**
     * The schedule in Cron format, see <a href="https://en.wikipedia.org/wiki/Cron">Cron</a>.
     */
    Optional<String> schedule();

    /**
     * The time zone for the job schedule. The default value is the local time of the kube-controller-manager.
     */
    Optional<String> timeZone();

    /**
     * ConcurrencyPolicy describes how the job will be handled.
     */
    @WithDefault("Allow")
    CronJobConcurrencyPolicy concurrencyPolicy();

    /**
     * Deadline in seconds for starting the job if it misses scheduled time for any reason.
     * Missed jobs executions will be counted as failed ones.
     */
    Optional<Long> startingDeadlineSeconds();

    /**
     * The number of failed finished jobs to retain. The default value is 1.
     */
    Optional<Integer> failedJobsHistoryLimit();

    /**
     * The number of successful finished jobs to retain. The default value is 3.
     */
    Optional<Integer> successfulJobsHistoryLimit();
}
