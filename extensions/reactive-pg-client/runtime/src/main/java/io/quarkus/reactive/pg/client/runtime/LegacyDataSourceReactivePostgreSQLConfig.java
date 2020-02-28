package io.quarkus.reactive.pg.client.runtime;

import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "reactive-pg-client", phase = ConfigPhase.RUN_TIME)
@Deprecated
public class LegacyDataSourceReactivePostgreSQLConfig {

    /**
     * @deprecated use quarkus.datasource.reactive.postgresql.cache-prepared-statements instead.
     */
    @ConfigItem
    @Deprecated
    public Optional<Boolean> cachePreparedStatements;

    /**
     * @deprecated use quarkus.datasource.reactive.postgresql.pipelining-limit instead.
     */
    @ConfigItem
    @Deprecated
    public OptionalInt pipeliningLimit;
}
