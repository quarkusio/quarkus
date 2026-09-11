package io.quarkus.flyway.mongodb.runtime;

import java.util.Set;

import org.flywaydb.core.Flyway;

/**
 * Holds a configured Flyway instance plus per-client startup flags and metadata.
 */
public record FlywayMongodbContainer(
        Flyway flyway,
        boolean baselineAtStart,
        boolean cleanAtStart,
        boolean cleanOnValidationError,
        boolean migrateAtStart,
        boolean repairAtStart,
        boolean validateAtStart,
        String clientName,
        boolean hasMigrations,
        Set<String> resourceLocations,
        boolean cleanDisabled) {
}
