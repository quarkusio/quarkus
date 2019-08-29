package io.quarkus.scheduler.runtime;

import java.time.Duration;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.runtime.annotations.ConvertWith;
import io.quarkus.runtime.configuration.DurationConverter;

@ConfigRoot(name = "scheduler", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class SchedulerBuildTimeConfig {
    /**
     * The instance name of the scheduler.
     */
    @ConfigItem(defaultValue = "DefaultQuartzScheduler")
    public String instanceName;

    /**
     * Scheduler job state store configuration
     */
    @ConfigItem
    public SchedulerJobStoreConfig stateStore;

    @ConfigGroup
    public static class SchedulerJobStoreConfig {

        /**
         * The number of duration by which a trigger must have missed its next-fire-time, in order for it to be
         * considered "misfired" and thus have its misfire instruction applied.
         */
        @ConfigItem(defaultValue = "1m")
        @ConvertWith(DurationConverter.class)
        public Duration misfireThreshold;

        /**
         * The frequency at which this instance "checks-in" with the other instances of the cluster
         * .This affects the rate of detecting failed instances.
         * Defaults to 20 seconds.
         */
        @ConfigItem
        @ConvertWith(DurationConverter.class)
        public Optional<Duration> clusterCheckingInterval;

        /**
         * Enable cluster mode or not.
         * Only supported when the job stateStore type is {@code jdbc}.
         */
        @ConfigItem
        public Optional<Boolean> clusterEnabled;

        /**
         * The type of stateStore to use. Possible values are: `in-memory`, `jdbc`.
         * Defaults to `in-memory`.
         * <ul>
         * <li>
         * If set to `in-memory`, the scheduler will use the {@link org.quartz.simpl.RAMJobStore} job store class
         * </li>
         * <li>
         * If set to `jdbc`, the scheduler will use the {@link org.quartz.impl.jdbcjobstore.JobStoreTX} job store
         * class. When using this configuration value make sure that you have the agroal datasource configured.
         * <p>
         * The Quarkus scheduler does not create the necessary scheduling tables in database automatically.
         * Thus you'll need to create them before application startup.
         * You can select sql scripts that corresponds to your sql driver from <a href=
         * "https://github.com/quartz-scheduler/quartz/tree/master/quartz-core/src/main/resources/org/quartz/impl/jdbcjobstore">
         * quartz sql script files</a>
         * </p>
         *
         * <p>
         * NOTE: This stateStore type is not supported in Native Image because of missing Object serialization see
         * https://github.com/oracle/graal/issues/460.
         * </p>
         * </li>
         * </ul>
         *
         */
        @ConfigItem(defaultValue = "in-memory", name = ConfigItem.PARENT)
        public StateStoreType type;

        /**
         * Scheduler datasource
         */
        @ConfigItem
        public SchedulerDatasourceConfig datasource;
    }

    @ConfigGroup
    public static class SchedulerDatasourceConfig {

        /**
         * The name of the datasource to use.
         * <p>
         * If not supplied it will default to default datasource.
         * See <a href="https://quarkus.io/guides/datasource-guide"> datasource guide </a> for more information
         * on how to configure datasource.
         */
        @ConfigItem
        public Optional<String> name;

        /**
         * The JDBC driver delegate class. This is not required when the job state store type is {@code in-memory}.
         * Defaults to {@link org.quartz.impl.jdbcjobstore.StdJDBCDelegate}
         */
        @ConfigItem(defaultValue = "org.quartz.impl.jdbcjobstore.StdJDBCDelegate")
        public String driverDelegateClass;
    }
}
