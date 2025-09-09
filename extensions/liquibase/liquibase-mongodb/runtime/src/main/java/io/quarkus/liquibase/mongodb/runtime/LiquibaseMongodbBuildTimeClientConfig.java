package io.quarkus.liquibase.mongodb.runtime;

import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

/**
 * The liquibase configuration by client.
 */
@ConfigGroup
public interface LiquibaseMongodbBuildTimeClientConfig {

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
