package io.quarkus.flyway.runtime.database;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

import java.util.Optional;
import java.util.OptionalInt;


/**
 * Config replicationg org.flywaydb.core.internal.database
 */
@ConfigGroup
public class PostgresConfig {
    /**
     * Whether or not transactional advisory locks should be used with PostgreSQL.
     * If false, session-level locks will be used instead.
     */
    @ConfigItem
    public Optional<Boolean> postgresqlTransactionalLock = Optional.of(false);
}
