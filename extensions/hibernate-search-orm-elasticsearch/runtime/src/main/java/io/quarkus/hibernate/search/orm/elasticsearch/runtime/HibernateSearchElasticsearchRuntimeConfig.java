package io.quarkus.hibernate.search.orm.elasticsearch.runtime;

import java.util.List;
import java.util.Map;

import io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithParentName;
import io.smallrye.config.WithUnnamedKey;

@ConfigMapping(prefix = "quarkus.hibernate-search-orm")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface HibernateSearchElasticsearchRuntimeConfig {

    /**
     * Configuration for persistence units.
     */
    @WithParentName
    @WithUnnamedKey(PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME)
    @ConfigDocMapKey("persistence-unit-name")
    Map<String, HibernateSearchElasticsearchRuntimeConfigPersistenceUnit> persistenceUnits();

    static String elasticsearchVersionPropertyKey(String persistenceUnitName, String backendName) {
        return backendPropertyKey(persistenceUnitName, backendName, null, "version");
    }

    static String extensionPropertyKey(String radical) {
        StringBuilder keyBuilder = new StringBuilder("quarkus.hibernate-search-orm.");
        keyBuilder.append(radical);
        return keyBuilder.toString();
    }

    static String mapperPropertyKey(String persistenceUnitName, String radical) {
        StringBuilder keyBuilder = new StringBuilder("quarkus.hibernate-search-orm.");
        if (!PersistenceUnitUtil.isDefaultPersistenceUnit(persistenceUnitName)) {
            keyBuilder.append("\"").append(persistenceUnitName).append("\".");
        }
        keyBuilder.append(radical);
        return keyBuilder.toString();
    }

    static List<String> mapperPropertyKeys(String persistenceUnitName, String radical) {
        if (PersistenceUnitUtil.isDefaultPersistenceUnit(persistenceUnitName)) {
            return List.of("quarkus.hibernate-search-orm." + radical);
        } else {
            // Two possible syntaxes: with or without quotes
            return List.of("quarkus.hibernate-search-orm.\"" + persistenceUnitName + "\"." + radical,
                    "quarkus.hibernate-search-orm." + persistenceUnitName + "." + radical);
        }
    }

    static String backendPropertyKey(String persistenceUnitName, String backendName, String indexName, String radical) {
        StringBuilder keyBuilder = new StringBuilder("quarkus.hibernate-search-orm.");
        if (!PersistenceUnitUtil.isDefaultPersistenceUnit(persistenceUnitName)) {
            keyBuilder.append("\"").append(persistenceUnitName).append("\".");
        }
        keyBuilder.append("elasticsearch.");
        if (backendName != null) {
            keyBuilder.append("\"").append(backendName).append("\".");
        }
        if (indexName != null) {
            keyBuilder.append("indexes.\"").append(indexName).append("\".");
        }
        keyBuilder.append(radical);
        return keyBuilder.toString();
    }

    static List<String> defaultBackendPropertyKeys(String persistenceUnitName, String radical) {
        return mapperPropertyKeys(persistenceUnitName, "elasticsearch." + radical);
    }
}
