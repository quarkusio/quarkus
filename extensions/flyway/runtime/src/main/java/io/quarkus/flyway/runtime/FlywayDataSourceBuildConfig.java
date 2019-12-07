package io.quarkus.flyway.runtime;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public final class FlywayDataSourceBuildConfig {

    private static final String DEFAULT_LOCATION = "db/migration";

    /**
     * Creates a {@link FlywayDataSourceBuildConfig} with default settings.
     * 
     * @return {@link FlywayDataSourceBuildConfig}
     */
    public static final FlywayDataSourceBuildConfig defaultConfig() {
        return new FlywayDataSourceBuildConfig();
    }

    /**
     * Comma-separated list of locations to scan recursively for migrations. The location type is determined by its prefix.
     * Unprefixed locations or locations starting with classpath: point to a package on the classpath and may contain both SQL
     * and Java-based migrations.
     * Locations starting with filesystem: point to a directory on the filesystem, may only contain SQL migrations and are only
     * scanned recursively down non-hidden directories.
     */
    @ConfigItem
    public Optional<List<String>> locations = Optional.of(Collections.singletonList(DEFAULT_LOCATION));
}
