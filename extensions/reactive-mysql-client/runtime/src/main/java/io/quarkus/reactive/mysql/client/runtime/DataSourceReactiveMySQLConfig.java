package io.quarkus.reactive.mysql.client.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.vertx.mysqlclient.SslMode;

@ConfigGroup
public class DataSourceReactiveMySQLConfig {

    /**
     * Whether prepared statements should be cached on the client side.
     *
     * @deprecated use {@code datasource.reactive.cache-prepared-statements} instead.
     */
    @ConfigItem
    @Deprecated
    public Optional<Boolean> cachePreparedStatements = Optional.empty();

    /**
     * Charset for connections.
     */
    @ConfigItem
    public Optional<String> charset = Optional.empty();

    /**
     * Collation for connections.
     */
    @ConfigItem
    public Optional<String> collation = Optional.empty();

    /**
     * Desired security state of the connection to the server.
     * <p>
     * See <a href="https://dev.mysql.com/doc/refman/8.0/en/connection-options.html#option_general_ssl-mode">MySQL Reference
     * Manual</a>.
     */
    @ConfigItem(defaultValueDocumentation = "disabled")
    public Optional<SslMode> sslMode = Optional.empty();
}
