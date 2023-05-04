package io.quarkus.liquibase.runtime;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

/**
 * The liquibase data source build time configuration
 */
@ConfigGroup
public final class LiquibaseDataSourceBuildTimeConfig {

    static final String DEFAULT_CHANGE_LOG = "db/changeLog.xml";

    /**
     * Creates a {@link LiquibaseDataSourceBuildTimeConfig} with default settings.
     *
     * @return {@link LiquibaseDataSourceBuildTimeConfig}
     */
    public static final LiquibaseDataSourceBuildTimeConfig defaultConfig() {
        LiquibaseDataSourceBuildTimeConfig defaultConfig = new LiquibaseDataSourceBuildTimeConfig();
        defaultConfig.changeLog = DEFAULT_CHANGE_LOG;
        return defaultConfig;
    }

    /**
     * The liquibase change log file. All included change log files in this file are scanned and add to the projects.
     */
    @ConfigItem(defaultValue = DEFAULT_CHANGE_LOG)
    public String changeLog;

    /**
     * Flag to enable / disable the migration using the generated init task Kubernetes resources.
     * This property is only relevant if the Quarkus Kubernetes/OpenShift extensions are present.
     *
     * The default value is `quarkus.liquibase.migrate-at-start`.
     */
    @ConfigItem(defaultValue = "${quarkus.liquibase.migrate-at-start:false}")
    public boolean migrateWithInitTask;

}
