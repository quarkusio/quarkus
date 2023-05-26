package io.quarkus.reactive.pg.client.runtime;

import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigDocDefault;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.vertx.pgclient.SslMode;

@ConfigGroup
public interface DataSourceReactivePostgreSQLConfig {

    /**
     * The maximum number of inflight database commands that can be pipelined.
     */
    OptionalInt pipeliningLimit();

    /**
     * SSL operating mode of the client.
     * <p>
     * See <a href="https://www.postgresql.org/docs/current/libpq-ssl.html#LIBPQ-SSL-PROTECTION">Protection Provided in
     * Different Modes</a>.
     */
    @ConfigDocDefault("disable")
    Optional<SslMode> sslMode();
}
