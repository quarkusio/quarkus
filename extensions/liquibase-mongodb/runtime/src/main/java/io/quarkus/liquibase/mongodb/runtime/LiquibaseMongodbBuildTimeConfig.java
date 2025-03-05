package io.quarkus.liquibase.mongodb.runtime;

import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * The liquibase configuration
 */
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
@ConfigMapping(prefix = "quarkus.liquibase-mongodb")
public interface LiquibaseMongodbBuildTimeConfig {

    /**
     * The change log file
     */
    @WithDefault("db/changeLog.xml")
    String changeLog();

    /**
     * The search path for DirectoryResourceAccessor
     */
    Optional<List<String>> searchPath();
}
