package io.quarkus.reactive.mysql.client.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.vertx.mysqlclient.SslMode;

@ConfigRoot(name = "datasource.reactive.mysql", phase = ConfigPhase.RUN_TIME)
public class DataSourceReactiveMySQLConfig {

    /**
     * Whether prepared statements should be cached on the client side.
     *
     * @deprecated use {@code datasource.reactive.cache-prepared-statements} instead.
     */
    @ConfigItem
    @Deprecated
    public Optional<Boolean> cachePreparedStatements;

    /**
     * Charset for connections.
     */
    @ConfigItem
    public Optional<String> charset;

    /**
     * Collation for connections.
     */
    @ConfigItem
    public Optional<String> collation;

    /**
     * Desired security state of the connection to the server.
     * <p>
     * See <a href="https://dev.mysql.com/doc/refman/8.0/en/connection-options.html#option_general_ssl-mode">MySQL Reference
     * Manual</a>.
     */
    @ConfigItem(defaultValueDocumentation = "disabled")
    public Optional<SslMode> sslMode;
}
