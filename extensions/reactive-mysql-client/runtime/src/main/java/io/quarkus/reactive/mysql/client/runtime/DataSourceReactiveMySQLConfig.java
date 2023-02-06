package io.quarkus.reactive.mysql.client.runtime;

import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.vertx.mysqlclient.MySQLAuthenticationPlugin;
import io.vertx.mysqlclient.SslMode;

@ConfigGroup
public class DataSourceReactiveMySQLConfig {

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

    /**
     * Connection timeout in seconds
     */
    @ConfigItem()
    public OptionalInt connectionTimeout = OptionalInt.empty();

    /**
     * The authentication plugin the client should use.
     * By default, it uses the plugin name specified by the server in the initial handshake packet.
     */
    @ConfigItem(defaultValueDocumentation = "default")
    public Optional<MySQLAuthenticationPlugin> authenticationPlugin = Optional.empty();

    /**
     * The maximum number of inflight database commands that can be pipelined.
     * By default, pipelining is disabled.
     */
    @ConfigItem
    public OptionalInt pipeliningLimit = OptionalInt.empty();

    /**
     * Whether to return the number of rows matched by the <em>WHERE</em> clause in <em>UPDATE</em> statements, instead of the
     * number of rows actually changed.
     */
    @ConfigItem(defaultValueDocumentation = "false")
    public Optional<Boolean> useAffectedRows = Optional.empty();
}
