package io.quarkus.jdbc.hsqldb.runtime;

import java.util.Map;

import io.agroal.api.configuration.supplier.AgroalDataSourceConfigurationSupplier;
import io.quarkus.agroal.runtime.AgroalConnectionConfigurer;
import io.quarkus.agroal.runtime.JdbcDriver;
import io.quarkus.datasource.common.runtime.DatabaseKind;

@JdbcDriver(DatabaseKind.HSQLDB)
public class HsqldbAgroalConnectionConfigurer implements AgroalConnectionConfigurer {

    @Override
    public void disableSslSupport(String databaseKind, AgroalDataSourceConfigurationSupplier dataSourceConfiguration,
            Map<String, String> additionalProperties) {
        // do not log anything for HSQLDB
    }

    @Override
    public void setExceptionSorter(String databaseKind, AgroalDataSourceConfigurationSupplier dataSourceConfiguration) {
        // Do not log a warning: we don't have an exception sorter for HSQLDB,
        // but there is nothing the user can do about it.
    }

}