package io.quarkus.flyway.runtime;

import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "flyway", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public final class FlywayBuildConfig {
    /**
     * Comma-separated list of locations to scan recursively for migrations. The location type is determined by its prefix.
     * Unprefixed locations or locations starting with classpath: point to a package on the classpath and may contain both SQL
     * and Java-based migrations.
     * Locations starting with filesystem: point to a directory on the filesystem, may only contain SQL migrations and are only
     * scanned recursively down non-hidden directories.
     */
    @ConfigItem
    public Optional<List<String>> locations;
}
