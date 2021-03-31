package io.quarkus.jdbc.oracle.runtime;

import java.util.Properties;

import io.agroal.api.configuration.supplier.AgroalDataSourceConfigurationSupplier;
import io.agroal.api.exceptionsorter.OracleExceptionSorter;
import io.quarkus.agroal.runtime.AgroalConnectionConfigurer;
import io.quarkus.agroal.runtime.JdbcDriver;
import io.quarkus.datasource.common.runtime.DatabaseKind;

@JdbcDriver(DatabaseKind.ORACLE)
public class OracleAgroalConnectionConfigurer implements AgroalConnectionConfigurer {

    /**
     * Oracle connection properties are documented here:
     * https://docs.oracle.com/en/database/oracle/oracle-database/21/jajdb/oracle/jdbc/OracleConnection.html#Transport_Layer_Security__TLS_SSL_
     *
     * SSL seems to be disabled by default, so rather than disabling it explicitly we check that the user didn't attempt to
     * enable it.
     */
    @Override
    public void disableSslSupport(String databaseKind, AgroalDataSourceConfigurationSupplier dataSourceConfiguration) {
        final Properties jdbcProperties = dataSourceConfiguration.connectionPoolConfiguration()
                .connectionFactoryConfiguration()
                .get()
                .jdbcProperties();
        final Object setting = jdbcProperties.get("oracle.net.authentication_services");
        if (setting != null && "SSL".equalsIgnoreCase(setting.toString())) {
            log.warnv(
                    "SSL support has been disabled, but one of the Oracle JDBC connections has been configured to use SSL. This will likely fail");
        }
    }

    @Override
    public void setExceptionSorter(String databaseKind, AgroalDataSourceConfigurationSupplier dataSourceConfiguration) {
        dataSourceConfiguration.connectionPoolConfiguration().exceptionSorter(new OracleExceptionSorter());
    }

}
