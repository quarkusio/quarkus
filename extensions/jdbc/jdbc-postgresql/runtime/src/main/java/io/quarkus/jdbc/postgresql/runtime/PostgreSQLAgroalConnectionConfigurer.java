package io.quarkus.jdbc.postgresql.runtime;

import java.time.Duration;
import java.util.Map;

import io.agroal.api.configuration.supplier.AgroalDataSourceConfigurationSupplier;
import io.agroal.api.exceptionsorter.PostgreSQLExceptionSorter;
import io.quarkus.agroal.runtime.AgroalConnectionConfigurer;
import io.quarkus.agroal.runtime.JdbcDriver;
import io.quarkus.datasource.common.runtime.DatabaseKind;

@JdbcDriver(DatabaseKind.POSTGRESQL)
public class PostgreSQLAgroalConnectionConfigurer implements AgroalConnectionConfigurer {

    @Override
    public void disableSslSupport(String databaseKind, AgroalDataSourceConfigurationSupplier dataSourceConfiguration,
            Map<String, String> additionalProperties) {
        dataSourceConfiguration.connectionPoolConfiguration().connectionFactoryConfiguration().jdbcProperty("sslmode",
                "disable");
    }

    @Override
    public void setExceptionSorter(String databaseKind, AgroalDataSourceConfigurationSupplier dataSourceConfiguration) {
        dataSourceConfiguration.connectionPoolConfiguration().exceptionSorter(new PostgreSQLExceptionSorter());
    }

    @Override
    public void setKeepAlive(String databaseKind, AgroalDataSourceConfigurationSupplier dataSourceConfiguration,
            Map<String, String> additionalJdbcProperties, boolean keepAlive) {
        // The PostgreSQL JDBC driver has its own keep-alive mechanism that can be enabled via a JDBC property. Default is false.
        // See https://jdbc.postgresql.org/documentation/use/#connection-parameters
        dataSourceConfiguration.connectionPoolConfiguration().connectionFactoryConfiguration().jdbcProperty("tcpKeepAlive",
                Boolean.toString(keepAlive));
    }

    @Override
    public void setReadTimeout(String databaseKind, AgroalDataSourceConfigurationSupplier dataSourceConfiguration,
            Map<String, String> additionalJdbcProperties, Duration timeout) {
        // The PostgreSQL JDBC driver uses the "socketTimeout" property to specify the socket timeout in seconds.
        // See https://jdbc.postgresql.org/documentation/use/#connection-parameters
        dataSourceConfiguration.connectionPoolConfiguration().connectionFactoryConfiguration().jdbcProperty("socketTimeout",
                Long.toString(timeout.getSeconds()));
    }
}
