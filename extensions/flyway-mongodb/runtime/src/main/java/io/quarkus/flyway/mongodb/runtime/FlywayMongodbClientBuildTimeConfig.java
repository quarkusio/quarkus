package io.quarkus.flyway.mongodb.runtime;

import java.util.List;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.configuration.TrimmedStringConverter;
import io.smallrye.config.WithConverter;
import io.smallrye.config.WithDefault;

/**
 * Per-client build-time configuration.
 */
@ConfigGroup
public interface FlywayMongodbClientBuildTimeConfig {

    String DEFAULT_LOCATION = "db/migration";
    String DEFAULT_MIGRATION_SUFFIX = ".js";

    /**
     * Comma-separated list of locations to scan recursively for migrations. The location type is determined by its prefix.
     * <p>
     * Unprefixed locations or locations starting with classpath: point to a package on the classpath and may contain both
     * JavaScript and JSON-based migrations.
     * <p>
     * Locations starting with filesystem: point to a directory on the filesystem, may only contain migration scripts and are
     * only scanned recursively down non-hidden directories.
     */
    @WithDefault(DEFAULT_LOCATION)
    List<@WithConverter(TrimmedStringConverter.class) String> locations();

    /**
     * Comma-separated list of file suffixes to recognize as migration scripts.
     * Defaults to {@code .js}. Set to {@code .json} to use JSON-format migrations instead;
     * the {@code flyway-database-nc-mongodb} connector does not support mixing
     * {@code .js} and {@code .json} migrations in the same project.
     */
    @WithDefault(DEFAULT_MIGRATION_SUFFIX)
    List<@WithConverter(TrimmedStringConverter.class) String> migrationSuffixes();
}
