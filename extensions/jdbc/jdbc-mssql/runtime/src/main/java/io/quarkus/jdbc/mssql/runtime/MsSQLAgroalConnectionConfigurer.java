package io.quarkus.jdbc.mssql.runtime;

import java.time.Duration;
import java.util.Map;

import io.agroal.api.configuration.supplier.AgroalDataSourceConfigurationSupplier;
import io.agroal.api.exceptionsorter.MSSQLExceptionSorter;
import io.quarkus.agroal.runtime.AgroalConnectionConfigurer;
import io.quarkus.agroal.runtime.JdbcDriver;
import io.quarkus.datasource.common.runtime.DatabaseKind;

@JdbcDriver(DatabaseKind.MSSQL)
public class MsSQLAgroalConnectionConfigurer implements AgroalConnectionConfigurer {

    @Override
    public void disableSslSupport(String databaseKind, AgroalDataSourceConfigurationSupplier dataSourceConfiguration,
            Map<String, String> additionalProperties) {
        dataSourceConfiguration.connectionPoolConfiguration().connectionFactoryConfiguration().jdbcProperty("encrypt", "false");
    }

    @Override
    public void setExceptionSorter(String databaseKind, AgroalDataSourceConfigurationSupplier dataSourceConfiguration) {
        dataSourceConfiguration.connectionPoolConfiguration().exceptionSorter(new MSSQLExceptionSorter());
    }

    @Override
    public void setReadTimeout(String databaseKind, AgroalDataSourceConfigurationSupplier dataSourceConfiguration,
            Map<String, String> additionalJdbcProperties, Duration timeout) {
        // MSSQL socket timeout is configured using the "socketTimeout" property, which is in milliseconds
        // See https://learn.microsoft.com/en-us/sql/connect/jdbc/understand-timeouts
        // https://learn.microsoft.com/en-us/sql/connect/jdbc/setting-the-connection-properties
        dataSourceConfiguration.connectionPoolConfiguration().connectionFactoryConfiguration()
                .jdbcProperty("socketTimeout", Long.toString(timeout.toMillis()));
    }
}
