package io.quarkus.jdbc.mariadb.runtime;

import java.time.Duration;
import java.util.Map;

import io.agroal.api.configuration.supplier.AgroalDataSourceConfigurationSupplier;
import io.agroal.api.exceptionsorter.MySQLExceptionSorter;
import io.quarkus.agroal.runtime.AgroalConnectionConfigurer;
import io.quarkus.agroal.runtime.JdbcDriver;
import io.quarkus.datasource.common.runtime.DatabaseKind;

@JdbcDriver(DatabaseKind.MARIADB)
public class MariaDBAgroalConnectionConfigurer implements AgroalConnectionConfigurer {

    @Override
    public void disableSslSupport(String databaseKind, AgroalDataSourceConfigurationSupplier dataSourceConfiguration,
            Map<String, String> additionalProperties) {
        dataSourceConfiguration.connectionPoolConfiguration().connectionFactoryConfiguration().jdbcProperty("useSSL", "false");
    }

    @Override
    public void setExceptionSorter(String databaseKind, AgroalDataSourceConfigurationSupplier dataSourceConfiguration) {
        // This exception sorter is apparently valid for MariaDB too.
        dataSourceConfiguration.connectionPoolConfiguration().exceptionSorter(new MySQLExceptionSorter());
    }

    @Override
    public void setKeepAlive(String databaseKind, AgroalDataSourceConfigurationSupplier dataSourceConfiguration,
            Map<String, String> additionalJdbcProperties, boolean keepAlive) {
        // MariaDB JDBC driver uses "tcpKeepAlive" property to enable keep-alive. Default is true.
        // See https://mariadb.com/docs/connectors/mariadb-connector-j/about-mariadb-connector-j
        dataSourceConfiguration.connectionPoolConfiguration().connectionFactoryConfiguration().jdbcProperty("tcpKeepAlive",
                Boolean.toString(keepAlive));
    }

    @Override
    public void setReadTimeout(String databaseKind, AgroalDataSourceConfigurationSupplier dataSourceConfiguration,
            Map<String, String> additionalJdbcProperties, Duration timeout) {
        // MariaDB JDBC driver uses "socketTimeout" property to configure socket timeout.
        // See https://mariadb.com/docs/connectors/mariadb-connector-j/about-mariadb-connector-j
        dataSourceConfiguration.connectionPoolConfiguration().connectionFactoryConfiguration().jdbcProperty("socketTimeout",
                Long.toString(timeout.toMillis()));
    }
}
