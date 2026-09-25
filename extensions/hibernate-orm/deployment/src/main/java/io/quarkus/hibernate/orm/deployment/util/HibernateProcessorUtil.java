package io.quarkus.hibernate.orm.deployment.util;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.hibernate.orm.deployment.HibernateOrmConfig;
import io.quarkus.hibernate.orm.deployment.spi.client.HibernateOrmClientDefinedBuildItem;
import io.quarkus.hibernate.orm.runtime.HibernateOrmRuntimeConfig;
import io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil;
import io.quarkus.hibernate.orm.runtime.migration.MultiTenancyStrategy;
import io.quarkus.runtime.configuration.ConfigurationException;

/**
 * Small helpers for Hibernate ORM deployment processors.
 *
 * @see HibernateProcessorSupport
 * @see io.quarkus.hibernate.orm.deployment.component.PersistenceUnitDefinitionSupport
 */
public final class HibernateProcessorUtil {

    private HibernateProcessorUtil() {
    }

    public static Optional<String> getDataSourceName(HibernateOrmConfig config, String persistenceUnitName) {
        Optional<String> result = config.persistenceUnits().get(persistenceUnitName).datasource();
        if (result.isEmpty() && PersistenceUnitUtil.isDefaultPersistenceUnit(persistenceUnitName)) {
            result = Optional.of(DataSourceUtil.DEFAULT_DATASOURCE_NAME);
        }
        return result;
    }

    public static <T> T findDataSourceWithName(String dataSourceName,
            List<T> dataSources,
            Function<T, String> nameExtractor) {
        return dataSources.stream()
                .filter(i -> dataSourceName.equals(nameExtractor.apply(i)))
                .findFirst()
                // If there is a configured datasource, it should have been requested and thus exist
                .orElseThrow(() -> new IllegalStateException(String.format(Locale.ROOT,
                        "Datasource %s was referenced but was not created -- this is a bug, please report it",
                        dataSourceName)));
    }

    public static boolean isHibernateValidatorPresent(Capabilities capabilities) {
        return capabilities.isPresent(Capability.HIBERNATE_VALIDATOR);
    }

    public static MultiTenancyStrategy getMultiTenancyStrategy(Optional<String> multitenancyStrategy) {
        return MultiTenancyStrategy
                .valueOf(multitenancyStrategy.orElse(MultiTenancyStrategy.NONE.name())
                        .toUpperCase(Locale.ROOT));
    }

    public static HibernateOrmClientDefinedBuildItem findClientWithName(String persistenceUnitName,
            String clientName,
            Map<String, List<HibernateOrmClientDefinedBuildItem>> clientsByName) {
        List<HibernateOrmClientDefinedBuildItem> clients = clientsByName.get(clientName);
        if (clients == null || clients.isEmpty()) {
            throw new ConfigurationException(String.format(Locale.ROOT,
                    "Persistence unit '%s' is configured with '%s',"
                            + " but no client extension can handle client '%s'."
                            + " Add an extension that provides this client"
                            + " (e.g. quarkus-mongodb-hibernate).",
                    persistenceUnitName,
                    HibernateOrmRuntimeConfig.puPropertyKey(persistenceUnitName, "client"),
                    clientName));
        }
        if (clients.size() > 1) {
            throw new ConfigurationException(String.format(Locale.ROOT,
                    "Persistence unit '%s' is configured with '%s',"
                            + " but multiple client extensions can handle client '%s'."
                            + " Make sure only one extension provides this client.",
                    persistenceUnitName,
                    HibernateOrmRuntimeConfig.puPropertyKey(persistenceUnitName, "client"),
                    clientName));
        }
        return clients.get(0);
    }

}
