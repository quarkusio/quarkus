package io.quarkus.agroal.runtime;

import java.time.Duration;
import java.util.Map;

import org.jboss.logging.Logger;

import io.agroal.api.configuration.supplier.AgroalDataSourceConfigurationSupplier;

public interface AgroalConnectionConfigurer {

    Logger log = Logger.getLogger(AgroalConnectionConfigurer.class.getName());

    /**
     * @deprecated use {@link #disableSslSupport(String, AgroalDataSourceConfigurationSupplier, Map)} instead
     */
    @Deprecated(since = "3.18", forRemoval = true)
    default void disableSslSupport(String databaseKind, AgroalDataSourceConfigurationSupplier dataSourceConfiguration) {
        log.warnv("Agroal does not support disabling SSL for database kind: {0}", databaseKind);
    }

    default void disableSslSupport(String databaseKind, AgroalDataSourceConfigurationSupplier dataSourceConfiguration,
            Map<String, String> additionalJdbcProperties) {
        disableSslSupport(databaseKind, dataSourceConfiguration);
    }

    default void setExceptionSorter(String databaseKind, AgroalDataSourceConfigurationSupplier dataSourceConfiguration) {
        log.warnv("Agroal does not support detecting if a connection is still usable after an exception for database kind: {0}",
                databaseKind);
    }

    default void setKeepAlive(String databaseKind, AgroalDataSourceConfigurationSupplier dataSourceConfiguration,
            Map<String, String> additionalJdbcProperties, boolean keepAlive) {
        log.warnv("Agroal does not support KeepAlive for database kind: {0}", databaseKind);
    }

    default void setReadTimeout(String databaseKind, AgroalDataSourceConfigurationSupplier dataSourceConfiguration,
            Map<String, String> additionalJdbcProperties, Duration timeout) {
        log.warnv("Agroal does not support setting the read timeout for database kind: {0}", databaseKind);
    }

}
