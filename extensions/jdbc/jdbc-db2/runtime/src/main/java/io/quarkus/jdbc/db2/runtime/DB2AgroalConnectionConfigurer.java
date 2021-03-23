package io.quarkus.jdbc.db2.runtime;

import io.agroal.api.configuration.supplier.AgroalDataSourceConfigurationSupplier;
import io.agroal.api.exceptionsorter.DB2ExceptionSorter;
import io.quarkus.agroal.runtime.AgroalConnectionConfigurer;
import io.quarkus.agroal.runtime.JdbcDriver;
import io.quarkus.datasource.common.runtime.DatabaseKind;

@JdbcDriver(DatabaseKind.DB2)
public class DB2AgroalConnectionConfigurer implements AgroalConnectionConfigurer {

    @Override
    public void setExceptionSorter(String databaseKind, AgroalDataSourceConfigurationSupplier dataSourceConfiguration) {
        dataSourceConfiguration.connectionPoolConfiguration().exceptionSorter(new DB2ExceptionSorter());
    }

}
