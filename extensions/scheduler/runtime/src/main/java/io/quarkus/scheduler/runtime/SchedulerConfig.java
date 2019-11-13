package io.quarkus.scheduler.runtime;

import com.cronutils.model.CronType;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.scheduler.Scheduled;

@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class SchedulerConfig {

    /**
     * The syntax used in CRON expressions.
     * 
     * @see Scheduled#cron()
     */
    @ConfigItem(defaultValue = "quartz")
    public CronType cronType;

}
