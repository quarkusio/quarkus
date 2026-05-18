package io.quarkus.datasource.common.runtime;

/**
 * Controls whether default database versions are applied when not explicitly configured.
 */
public enum DbVersionDefaults {
    /**
     * Apply default database versions (recent stable versions for each db-kind).
     * This is the default behavior.
     */
    RECENT,

    /**
     * Do not apply default database versions.
     * Requires explicit configuration of db-version for each datasource.
     */
    COMPATIBLE
}
