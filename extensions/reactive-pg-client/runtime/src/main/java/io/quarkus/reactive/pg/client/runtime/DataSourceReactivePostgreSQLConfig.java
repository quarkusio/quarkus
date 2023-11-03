package io.quarkus.reactive.pg.client.runtime;

import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigDocDefault;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;
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

    /**
     * Level 7 proxies can load balance queries on several connections to the actual database.
     * When it happens, the client can be confused by the lack of session affinity and unwanted errors can happen like
     * ERROR: unnamed prepared statement does not exist (26000).
     * See <a href="https://vertx.io/docs/vertx-pg-client/java/#_using_a_level_7_proxy">Using a level 7 proxy</a>
     */
    @WithDefault("false")
    boolean useLayer7Proxy();
}
