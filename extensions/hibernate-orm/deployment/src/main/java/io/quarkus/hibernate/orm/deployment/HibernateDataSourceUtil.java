package io.quarkus.hibernate.orm.deployment;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil;

public class HibernateDataSourceUtil {
    public static <T> Optional<T> findDataSourceWithNameDefault(String persistenceUnitName,
            List<T> datasSources,
            Function<T, String> nameExtractor,
            Function<T, Boolean> defaultExtractor,
            Optional<String> datasource) {
        if (datasource.isPresent()) {
            String dataSourceName = datasource.get();
            return datasSources.stream()
                    .filter(i -> dataSourceName.equals(nameExtractor.apply(i)))
                    .findFirst();
        } else if (PersistenceUnitUtil.isDefaultPersistenceUnit(persistenceUnitName)) {
            return datasSources.stream()
                    .filter(i -> defaultExtractor.apply(i))
                    .findFirst();
        } else {
            // if it's not the default persistence unit, we mandate an explicit datasource to prevent common errors
            return Optional.empty();
        }
    }
}
