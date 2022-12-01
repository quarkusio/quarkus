package io.quarkus.jdbc.mariadb.runtime;

import io.agroal.api.configuration.supplier.AgroalDataSourceConfigurationSupplier;
import io.agroal.api.exceptionsorter.MySQLExceptionSorter;
import io.quarkus.agroal.runtime.AgroalConnectionConfigurer;
import io.quarkus.agroal.runtime.JdbcDriver;
import io.quarkus.datasource.common.runtime.DatabaseKind;

@JdbcDriver(DatabaseKind.MARIADB)
public class MariaDBAgroalConnectionConfigurer implements AgroalConnectionConfigurer {

    @Override
    public void disableSslSupport(String databaseKind, AgroalDataSourceConfigurationSupplier dataSourceConfiguration) {
        dataSourceConfiguration.connectionPoolConfiguration().connectionFactoryConfiguration().jdbcProperty("useSSL", "false");
    }

    @Override
    public void setExceptionSorter(String databaseKind, AgroalDataSourceConfigurationSupplier dataSourceConfiguration) {
        // This exception sorter is apparently valid for MariaDB too.
        dataSourceConfiguration.connectionPoolConfiguration().exceptionSorter(new MySQLExceptionSorter());
    }

}
