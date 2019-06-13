package io.quarkus.reactive.pg.client.runtime;

import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "reactive-pg-client", phase = ConfigPhase.RUN_TIME)
public class PgPoolConfig {

    /**
     * Whether prepared statements should be cached on the client side.
     */
    @ConfigItem
    public Optional<Boolean> cachePreparedStatements;

    /**
     * The maximum number of inflight database commands that can be pipelined.
     */
    @ConfigItem
    public OptionalInt pipeliningLimit;
}
