package io.quarkus.quartz.runtime;

import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
@ConfigMapping(prefix = "quarkus.quartz")
public interface QuartzBuildTimeConfig {
    /**
     * Enable cluster mode or not.
     * <p>
     * If enabled make sure to set the appropriate cluster properties.
     */
    @WithDefault("false")
    boolean clustered();

    /**
     * The frequency (in milliseconds) at which the scheduler instance checks-in with other instances of the cluster.
     * <p>
     * Ignored if using a `ram` store i.e {@link StoreType#RAM}.
     */
    @WithDefault("15000")
    long clusterCheckinInterval();

    /**
     * The type of store to use.
     * <p>
     * When using {@link StoreType#JDBC_CMT} or {@link StoreType#JDBC_TX} configuration values make sure that you have the
     * datasource configured. See <a href="https://quarkus.io/guides/datasource"> Configuring your datasource</a> for more
     * information.
     * <p>
     * To create Quartz tables, you can perform a schema migration via the <a href="https://quarkus.io/guides/flyway"> Flyway
     * extension</a> using a SQL script matching your database picked from <a href=
     * "https://github.com/quartz-scheduler/quartz/blob/master/quartz-core/src/main/resources/org/quartz/impl/jdbcjobstore">Quartz
     * repository</a>.
     */
    @WithDefault("ram")
    StoreType storeType();

    /**
     * The class name of the thread pool implementation to use.
     * <p>
     * It's important to bear in mind that Quartz threads are not used to execute scheduled methods, instead the regular Quarkus
     * thread pool is used by default. See also {@code quarkus.quartz.run-blocking-scheduled-method-on-quartz-thread}.
     */
    @WithDefault("org.quartz.simpl.SimpleThreadPool")
    String threadPoolClass();

    /**
     * The name of the datasource to use.
     * <p>
     * Ignored if using a `ram` store i.e {@link StoreType#RAM}.
     * <p>
     * Optionally needed when using the `jdbc-tx` or `jdbc-cmt` store types.
     * If not specified, defaults to using the default datasource.
     */
    @WithName("datasource")
    Optional<String> dataSourceName();

    /**
     * If set to true, defers datasource check to runtime.
     * False by default.
     * <p>
     * Used in combination with runtime configuration {@link QuartzRuntimeConfig#deferredDatasourceName()}.
     * <p>
     * It is considered a configuration error to specify a datasource via {@link QuartzBuildTimeConfig#dataSourceName()} along
     * with setting this property to {@code true}.
     */
    @WithDefault("false")
    boolean deferDatasourceCheck();

    /**
     * The prefix for quartz job store tables.
     * <p>
     * Ignored if using a `ram` store i.e {@link StoreType#RAM}
     */
    @WithDefault("QRTZ_")
    String tablePrefix();

    /**
     * The SQL string that selects a row in the "LOCKS" table and places a lock on the row.
     * <p>
     * Ignored if using a `ram` store i.e {@link StoreType#RAM}.
     * <p>
     * If not set, the default value of Quartz applies, for which the "{0}" is replaced during run-time with the
     * `table-prefix`, the "{1}" with the `instance-name`.
     * <p>
     * An example SQL string `SELECT * FROM {0}LOCKS WHERE SCHED_NAME = {1} AND LOCK_NAME = ? FOR UPDATE`
     */
    Optional<String> selectWithLockSql();

    /**
     * Allows users to specify fully qualified class name for a custom JDBC driver delegate.
     * <p>
     * This property is optional and leaving it empty will result in Quarkus automatically choosing appropriate default
     * driver delegate implementation.
     * <p>
     * Note that any custom implementation has to be a subclass of existing Quarkus implementation such as
     * {@link io.quarkus.quartz.runtime.jdbc.QuarkusPostgreSQLDelegate} or
     * {@link io.quarkus.quartz.runtime.jdbc.QuarkusMSSQLDelegate}
     */
    Optional<String> driverDelegate();

    /**
     * Instructs JDBCJobStore to serialize JobDataMaps in the BLOB column.
     * <p>
     * Ignored if using a `ram` store i.e {@link StoreType#RAM}.
     * <p>
     * If this is set to `true`, the JDBCJobStore will store the JobDataMaps in their serialize form in the BLOB Column.
     * This is useful when you want to store complex JobData objects other than String.
     * This is equivalent of setting `org.quartz.jobStore.useProperties` to `false`.
     * <b>NOTE: When this option is set to `true`, all the non-String classes used in JobDataMaps have to be registered
     * for serialization when building a native image</b>
     * <p>
     * If this is set to `false` (the default), the values can be stored as name-value pairs rather than storing more complex
     * objects in their serialized form in the BLOB column.
     * This can be handy, as you avoid the class versioning issues that can arise from serializing your non-String classes into
     * a BLOB.
     * This is equivalent of setting `org.quartz.jobStore.useProperties` to `true`.
     */
    @WithDefault("false")
    boolean serializeJobData();

    /**
     * Instance ID generators.
     */
    @ConfigDocMapKey("generator-name")
    @ConfigDocSection
    Map<String, QuartzExtensionPointConfig> instanceIdGenerators();

    /**
     * Trigger listeners.
     */
    @ConfigDocMapKey("listener-name")
    @ConfigDocSection
    Map<String, QuartzExtensionPointConfig> triggerListeners();

    /**
     * Job listeners.
     */
    @ConfigDocMapKey("listener-name")
    @ConfigDocSection
    Map<String, QuartzExtensionPointConfig> jobListeners();

    /**
     * Plugins.
     */
    @ConfigDocMapKey("plugin-name")
    @ConfigDocSection
    Map<String, QuartzExtensionPointConfig> plugins();
}
