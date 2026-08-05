package io.quarkus.jdbc.mysql.runtime;

import java.time.Duration;
import java.util.Map;

import io.agroal.api.configuration.supplier.AgroalDataSourceConfigurationSupplier;
import io.agroal.api.exceptionsorter.MySQLExceptionSorter;
import io.quarkus.agroal.runtime.AgroalConnectionConfigurer;
import io.quarkus.agroal.runtime.JdbcDriver;
import io.quarkus.datasource.common.runtime.DatabaseKind;

@JdbcDriver(DatabaseKind.MYSQL)
public class MySQLAgroalConnectionConfigurer implements AgroalConnectionConfigurer {

    @Override
    public void disableSslSupport(String databaseKind, AgroalDataSourceConfigurationSupplier dataSourceConfiguration,
            Map<String, String> additionalProperties) {
        dataSourceConfiguration.connectionPoolConfiguration().connectionFactoryConfiguration().jdbcProperty("useSSL", "false");
    }

    @Override
    public void setExceptionSorter(String databaseKind, AgroalDataSourceConfigurationSupplier dataSourceConfiguration) {
        dataSourceConfiguration.connectionPoolConfiguration().exceptionSorter(new MySQLExceptionSorter());
    }

    @Override
    public void setKeepAlive(String databaseKind, AgroalDataSourceConfigurationSupplier dataSourceConfiguration,
            Map<String, String> additionalJdbcProperties, boolean keepAlive) {
        // The MySQL JDBC driver has its own keep-alive mechanism that can be enabled via a JDBC property
        // https://dev.mysql.com/doc/connector-j/en/connector-j-connp-props-networking.html
        dataSourceConfiguration.connectionPoolConfiguration().connectionFactoryConfiguration()
                .jdbcProperty("tcpKeepAlive", Boolean.toString(keepAlive));
    }

    @Override
    public void setReadTimeout(String databaseKind, AgroalDataSourceConfigurationSupplier dataSourceConfiguration,
            Map<String, String> additionalJdbcProperties, Duration timeout) {
        // https://dev.mysql.com/doc/connector-j/en/connector-j-connp-props-networking.html
        dataSourceConfiguration.connectionPoolConfiguration().connectionFactoryConfiguration()
                .jdbcProperty("socketTimeout", Long.toString(timeout.toMillis()));
    }
}
