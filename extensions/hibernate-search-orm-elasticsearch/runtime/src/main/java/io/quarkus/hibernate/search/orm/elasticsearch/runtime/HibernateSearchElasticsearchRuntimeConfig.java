package io.quarkus.hibernate.search.orm.elasticsearch.runtime;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "hibernate-search-orm", phase = ConfigPhase.RUN_TIME)
public class HibernateSearchElasticsearchRuntimeConfig {

    /**
     * Configuration for the default persistence unit.
     */
    @ConfigItem(name = ConfigItem.PARENT)
    public HibernateSearchElasticsearchRuntimeConfigPersistenceUnit defaultPersistenceUnit;

    /**
     * Configuration for additional named persistence units.
     */
    @ConfigDocSection
    @ConfigDocMapKey("persistence-unit-name")
    @ConfigItem(name = ConfigItem.PARENT)
    public Map<String, HibernateSearchElasticsearchRuntimeConfigPersistenceUnit> persistenceUnits;

    public Map<String, HibernateSearchElasticsearchRuntimeConfigPersistenceUnit> getAllPersistenceUnitConfigsAsMap() {
        Map<String, HibernateSearchElasticsearchRuntimeConfigPersistenceUnit> map = new TreeMap<>();
        if (defaultPersistenceUnit != null) {
            map.put(PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME, defaultPersistenceUnit);
        }
        map.putAll(persistenceUnits);
        return map;
    }

    public static String elasticsearchVersionPropertyKey(String persistenceUnitName, String backendName) {
        return backendPropertyKey(persistenceUnitName, backendName, null, "version");
    }

    public static String mapperPropertyKey(String persistenceUnitName, String radical) {
        StringBuilder keyBuilder = new StringBuilder("quarkus.hibernate-search-orm.");
        if (!PersistenceUnitUtil.isDefaultPersistenceUnit(persistenceUnitName)) {
            keyBuilder.append("\"").append(persistenceUnitName).append("\".");
        }
        keyBuilder.append(radical);
        return keyBuilder.toString();
    }

    public static String backendPropertyKey(String persistenceUnitName, String backendName, String indexName, String radical) {
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

    public static List<String> defaultBackendPropertyKeys(String persistenceUnitName, String radical) {
        if (PersistenceUnitUtil.isDefaultPersistenceUnit(persistenceUnitName)) {
            return List.of("quarkus.hibernate-search-orm.elasticsearch." + radical);
        } else {
            // Two possible syntaxes: with or without quotes
            return List.of("quarkus.hibernate-search-orm.\"" + persistenceUnitName + "\".elasticsearch." + radical,
                    "quarkus.hibernate-search-orm." + persistenceUnitName + ".elasticsearch." + radical);
        }
    }
}
