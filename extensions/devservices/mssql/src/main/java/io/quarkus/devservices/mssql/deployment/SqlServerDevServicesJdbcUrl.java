package io.quarkus.devservices.mssql.deployment;

import io.quarkus.devservices.common.DevServicesHostUtil;

/**
 * Builds JDBC URLs for SQL Server dev services.
 * <p>
 * The Microsoft JDBC driver does not support raw IPv6 literals in the URL authority
 * ({@code jdbc:sqlserver://host:port}). For IPv6, use the {@code serverName} and {@code port}
 * connection properties instead. See
 * <a href="https://learn.microsoft.com/en-us/sql/connect/jdbc/building-the-connection-url">
 * Building the connection URL</a>.
 */
final class SqlServerDevServicesJdbcUrl {

    private static final String JDBC_PREFIX = "jdbc:sqlserver://";

    private SqlServerDevServicesJdbcUrl() {
    }

    static String build(String host, int port, String additionalUrlParams) {
        if (DevServicesHostUtil.isIPv6Literal(host)) {
            return JDBC_PREFIX + ";serverName=" + host + ";port=" + port + additionalUrlParams;
        }
        return JDBC_PREFIX + DevServicesHostUtil.formatHostAndPort(host, port) + additionalUrlParams;
    }
}
