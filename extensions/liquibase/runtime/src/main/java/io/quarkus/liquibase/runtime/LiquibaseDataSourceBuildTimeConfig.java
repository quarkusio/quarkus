package io.quarkus.liquibase.runtime;

import java.util.List;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

/**
 * The liquibase data source build time configuration
 */
@ConfigGroup
public final class LiquibaseDataSourceBuildTimeConfig {

    static final String DEFAULT_CHANGE_LOG = "db/changeLog.xml";
    static final String DEFAULT_SEARCH_PATH = "/";

    /**
     * Creates a {@link LiquibaseDataSourceBuildTimeConfig} with default settings.
     *
     * @return {@link LiquibaseDataSourceBuildTimeConfig}
     */
    public static final LiquibaseDataSourceBuildTimeConfig defaultConfig() {
        LiquibaseDataSourceBuildTimeConfig defaultConfig = new LiquibaseDataSourceBuildTimeConfig();
        defaultConfig.changeLog = DEFAULT_CHANGE_LOG;
        defaultConfig.searchPath = List.of(DEFAULT_SEARCH_PATH);
        return defaultConfig;
    }

    /**
     * The liquibase change log file. All included change log files in this file are scanned and add to the projects.
     */
    @ConfigItem(defaultValue = DEFAULT_CHANGE_LOG)
    public String changeLog;

    /**
     * The search path for DirectoryResourceAccessor
     */
    @ConfigItem(defaultValue = "/")
    public List<String> searchPath;
}
