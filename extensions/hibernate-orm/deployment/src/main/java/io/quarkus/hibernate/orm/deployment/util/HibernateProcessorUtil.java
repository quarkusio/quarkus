package io.quarkus.hibernate.orm.deployment.util;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.hibernate.orm.deployment.HibernateOrmConfig;
import io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil;
import io.quarkus.hibernate.orm.runtime.migration.MultiTenancyStrategy;

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

}
