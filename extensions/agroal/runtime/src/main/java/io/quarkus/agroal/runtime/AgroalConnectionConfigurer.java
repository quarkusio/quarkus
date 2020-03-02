package io.quarkus.agroal.runtime;

import org.jboss.logging.Logger;

import io.agroal.api.configuration.supplier.AgroalDataSourceConfigurationSupplier;

public interface AgroalConnectionConfigurer {

    Logger log = Logger.getLogger(AgroalConnectionConfigurer.class.getName());

    default void disableSslSupport(String databaseKind, AgroalDataSourceConfigurationSupplier dataSourceConfiguration) {
        log.warnv("Agroal does not support disabling SSL for database kind: {0}", databaseKind);
    }

}
