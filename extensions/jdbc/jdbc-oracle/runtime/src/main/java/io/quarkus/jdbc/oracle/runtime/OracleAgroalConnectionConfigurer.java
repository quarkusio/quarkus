package io.quarkus.jdbc.oracle.runtime;

import io.agroal.api.configuration.supplier.AgroalDataSourceConfigurationSupplier;
import io.agroal.api.exceptionsorter.OracleExceptionSorter;
import io.quarkus.agroal.runtime.AgroalConnectionConfigurer;
import io.quarkus.agroal.runtime.JdbcDriver;
import io.quarkus.datasource.common.runtime.DatabaseKind;

@JdbcDriver(DatabaseKind.ORACLE)
public class OracleAgroalConnectionConfigurer implements AgroalConnectionConfigurer {

    @Override
    public void setExceptionSorter(String databaseKind, AgroalDataSourceConfigurationSupplier dataSourceConfiguration) {
        dataSourceConfiguration.connectionPoolConfiguration().exceptionSorter(new OracleExceptionSorter());
    }

}
