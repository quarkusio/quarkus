package io.quarkus.jdbc.runtime.runtime;

import java.sql.Driver;
import java.util.Set;

import javax.sql.DataSource;
import javax.sql.XADataSource;

import io.quarkus.agroal.runtime.AgroalDriverResolver;
import io.quarkus.agroal.runtime.DataSourcesJdbcBuildTimeConfig;
import io.quarkus.agroal.runtime.JdbcDriver;
import io.quarkus.agroal.runtime.TransactionIntegration;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.runtime.configuration.ConfigurationException;

/**
 * Resolves the JDBC driver of datasources of database kind {@link RuntimeJdbc#DB_KIND} at runtime,
 * from the {@code quarkus.datasource[."datasource-name"].jdbc-runtime.driver} configuration
 * property, instead of the driver class resolved at build time.
 */
@JdbcDriver(RuntimeJdbc.DB_KIND)
public class RuntimeJdbcDriverResolver implements AgroalDriverResolver {

    private static final String DRIVER_PROPERTY_RADICAL = "jdbc-runtime.driver";

    private final DataSourcesJdbcRuntimeDriverConfig runtimeDriverConfig;
    private final DataSourcesJdbcBuildTimeConfig jdbcBuildTimeConfig;

    public RuntimeJdbcDriverResolver(final DataSourcesJdbcRuntimeDriverConfig runtimeDriverConfig,
            final DataSourcesJdbcBuildTimeConfig jdbcBuildTimeConfig) {
        this.runtimeDriverConfig = runtimeDriverConfig;
        this.jdbcBuildTimeConfig = jdbcBuildTimeConfig;
    }

    @Override
    public String resolveDriverClassName(final String dataSourceName, final String buildTimeDriverClass) {
        final var driverProperty = DataSourceUtil.dataSourcePropertyKey(dataSourceName, DRIVER_PROPERTY_RADICAL);
        final var driverClassName = runtimeDriverConfig.dataSources().get(dataSourceName).jdbcRuntime().driver()
                .orElseThrow(() -> new ConfigurationException(
                        "Datasource '" + dataSourceName + "' uses the '" + RuntimeJdbc.DB_KIND + "' database kind"
                                + " but does not configure the JDBC driver class to load."
                                + " To solve this, set the '" + driverProperty
                                + "' configuration property to the fully qualified name of a JDBC driver class"
                                + " present in the application classpath.",
                        Set.of(driverProperty)));
        validate(dataSourceName, driverProperty, driverClassName);
        return driverClassName;
    }

    private void validate(final String dataSourceName, final String driverProperty, final String driverClassName) {
        final Class<?> driverClass;
        try {
            driverClass = Class.forName(driverClassName, true, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            throw new ConfigurationException(
                    "Unable to load the JDBC driver class '" + driverClassName + "' configured with '" + driverProperty
                            + "' for datasource '" + dataSourceName
                            + "'. Ensure the driver is present in the application classpath.",
                    e, Set.of(driverProperty));
        }

        final var xa = jdbcBuildTimeConfig.dataSources().get(dataSourceName).jdbc()
                .transactions() == TransactionIntegration.XA;
        if (xa) {
            if (!XADataSource.class.isAssignableFrom(driverClass)) {
                throw new ConfigurationException(
                        "Datasource '" + dataSourceName + "' uses XA transactions but the driver class '" + driverClassName
                                + "' configured with '" + driverProperty + "' is not an implementation of "
                                + XADataSource.class.getName() + ".",
                        Set.of(driverProperty));
            }
        } else if (!Driver.class.isAssignableFrom(driverClass) && !DataSource.class.isAssignableFrom(driverClass)) {
            throw new ConfigurationException(
                    "The driver class '" + driverClassName + "' configured with '" + driverProperty + "' for datasource '"
                            + dataSourceName + "' is neither an implementation of " + Driver.class.getName() + " nor of "
                            + DataSource.class.getName() + ".",
                    Set.of(driverProperty));
        }
    }
}
