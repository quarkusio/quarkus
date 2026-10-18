package io.quarkus.flyway.runtime;

import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.configuration.TrimmedStringConverter;
import io.smallrye.config.WithConverter;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface FlywayDataSourceBuildTimeConfig {

    String DEFAULT_LOCATION = "db/migration";

    /**
     * Comma-separated list of locations to scan recursively for migrations. The location type is determined by its prefix.
     * <p>
     * Unprefixed locations or locations starting with classpath: point to a package on the classpath and may contain both SQL
     * and Java-based migrations.
     * <p>
     * Locations starting with filesystem: point to a directory on the filesystem, may only contain SQL migrations and are only
     * scanned recursively down non-hidden directories.
     */
    @WithDefault(DEFAULT_LOCATION)
    List<@WithConverter(TrimmedStringConverter.class) String> locations();

    /**
     * Comma-separated list of fully qualified class names of Callback implementations
     * to use to hook into the Flyway lifecycle.
     * The {@link org.flywaydb.core.api.callback.Callback} subclass must have a no-args constructor and must not be abstract.
     * These classes must also not have any fields that hold state (unless that state is initialized in the constructor).
     */
    Optional<List<String>> callbacks();
}
