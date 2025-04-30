package io.quarkus.liquibase.runtime;

import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

/**
 * The liquibase data source build time configuration
 */
@ConfigGroup
public interface LiquibaseDataSourceBuildTimeConfig {

    static final String DEFAULT_CHANGE_LOG = "db/changeLog.xml";

    /**
     * The liquibase change log file. All included change log files in this file are scanned and add to the projects.
     */
    @WithDefault(DEFAULT_CHANGE_LOG)
    String changeLog();

    /**
     * The search path for DirectoryResourceAccessor
     */
    Optional<List<String>> searchPath();
}
