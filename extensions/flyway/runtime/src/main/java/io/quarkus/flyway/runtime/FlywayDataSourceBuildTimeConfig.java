package io.quarkus.flyway.runtime;

import java.util.Arrays;
import java.util.List;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public final class FlywayDataSourceBuildTimeConfig {

    private static final String DEFAULT_LOCATION = "db/migration";

    /**
     * Creates a {@link FlywayDataSourceBuildTimeConfig} with default settings.
     *
     * @return {@link FlywayDataSourceBuildTimeConfig}
     */
    public static final FlywayDataSourceBuildTimeConfig defaultConfig() {
        FlywayDataSourceBuildTimeConfig defaultConfig = new FlywayDataSourceBuildTimeConfig();
        defaultConfig.locations = Arrays.asList(DEFAULT_LOCATION);
        return defaultConfig;
    }

    /**
     * Comma-separated list of locations to scan recursively for migrations. The location type is determined by its prefix.
     * <p>
     * Unprefixed locations or locations starting with classpath: point to a package on the classpath and may contain both SQL
     * and Java-based migrations.
     * <p>
     * Locations starting with filesystem: point to a directory on the filesystem, may only contain SQL migrations and are only
     * scanned recursively down non-hidden directories.
     */
    @ConfigItem(defaultValue = DEFAULT_LOCATION)
    public List<String> locations;
}
