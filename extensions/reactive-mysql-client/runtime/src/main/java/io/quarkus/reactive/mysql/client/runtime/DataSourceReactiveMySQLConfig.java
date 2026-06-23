package io.quarkus.reactive.mysql.client.runtime;

import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigDocDefault;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.vertx.mysqlclient.MySQLAuthenticationPlugin;
import io.vertx.mysqlclient.SslMode;

@ConfigGroup
public interface DataSourceReactiveMySQLConfig {

    /**
     * Charset for connections.
     */
    Optional<String> charset();

    /**
     * Collation for connections.
     */
    Optional<String> collation();

    /**
     * Desired security state of the connection to the server.
     * <p>
     * See <a href="https://dev.mysql.com/doc/refman/8.0/en/connection-options.html#option_general_ssl-mode">MySQL Reference
     * Manual</a>.
     */
    @ConfigDocDefault("disabled")
    Optional<SslMode> sslMode();

    /**
     * Connection timeout in seconds
     *
     * @deprecated Use {@code quarkus.datasource.reactive.connection-timeout} instead.
     */
    @Deprecated
    OptionalInt connectionTimeout();

    /**
     * The authentication plugin the client should use.
     * By default, it uses the plugin name specified by the server in the initial handshake packet.
     */
    @ConfigDocDefault("default")
    Optional<MySQLAuthenticationPlugin> authenticationPlugin();

    /**
     * The maximum number of inflight database commands that can be pipelined.
     * By default, pipelining is disabled.
     */
    OptionalInt pipeliningLimit();

    /**
     * Whether to return the number of rows matched by the <em>WHERE</em> clause in <em>UPDATE</em> statements, instead of the
     * number of rows actually changed.
     */
    @ConfigDocDefault("false")
    Optional<Boolean> useAffectedRows();

    /**
     * Set the Java charset to use to encode query string and parameter values.
     * The charset is UTF-8 by default.
     */
    @ConfigDocDefault("UTF-8")
    Optional<String> characterEncoding();

    /**
     * Path to the server RSA public key file.
     * <p>
     * Used to securely encrypt the password during login when using SHA-256
     * authentication plugins (e.g., {@code caching_sha2_password}) over unencrypted
     * (non-TLS) connections. Specifying this locally prevents Man-in-the-Middle
     * attacks by avoiding automatic public key retrieval from the server.
     */
    Optional<String> serverRsaPublicKeyPath();

    /**
     * The server RSA public key content.
     * <p>
     * Used to securely encrypt the password during login when using SHA-256
     * authentication plugins (e.g., {@code caching_sha2_password}) over unencrypted
     * (non-TLS) connections. Specifying this locally prevents Man-in-the-Middle
     * attacks by avoiding automatic public key retrieval from the server.
     * <p>
     * If set, takes precedence over {@code server-rsa-public-key-path}.
     */
    Optional<String> serverRsaPublicKeyValue();
}
