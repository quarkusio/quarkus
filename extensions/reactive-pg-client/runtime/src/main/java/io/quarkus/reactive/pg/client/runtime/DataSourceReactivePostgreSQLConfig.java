package io.quarkus.reactive.pg.client.runtime;

import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.vertx.pgclient.SslMode;

@ConfigRoot(name = "datasource.reactive.postgresql", phase = ConfigPhase.RUN_TIME)
public class DataSourceReactivePostgreSQLConfig {

    /**
     * Whether prepared statements should be cached on the client side.
     * 
     * @deprecated use {@code datasource.reactive.cache-prepared-statements} instead.
     */
    @ConfigItem
    @Deprecated
    public Optional<Boolean> cachePreparedStatements;

    /**
     * The maximum number of inflight database commands that can be pipelined.
     */
    @ConfigItem
    public OptionalInt pipeliningLimit;

    /**
     * SSL operating mode of the client.
     * <p>
     * See <a href="https://www.postgresql.org/docs/current/libpq-ssl.html#LIBPQ-SSL-PROTECTION">Protection Provided in
     * Different Modes</a>.
     */
    @ConfigItem(defaultValueDocumentation = "disable")
    public Optional<SslMode> sslMode;
}
