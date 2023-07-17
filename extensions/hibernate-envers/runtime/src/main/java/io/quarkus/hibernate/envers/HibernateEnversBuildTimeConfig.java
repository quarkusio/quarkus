package io.quarkus.hibernate.envers;

import java.util.Map;
import java.util.TreeMap;

import io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class HibernateEnversBuildTimeConfig {
    /**
     * Whether Hibernate Envers is enabled <strong>during the build</strong>.
     *
     * If Hibernate Envers is disabled during the build, all processing related to Hibernate Envers will be skipped,
     * and the audit entities will not be added to the Hibernate ORM metamodel
     * nor to the database schema that Hibernate ORM generates,
     * but it will not be possible to use Hibernate Envers at runtime:
     * `quarkus.hibernate-envers.active` will default to `false` and setting it to `true` will lead to an error.
     *
     * @asciidoclet
     */
    @ConfigItem(defaultValue = "true")
    public boolean enabled;

    /**
     * Configuration for the default persistence unit.
     */
    @ConfigItem(name = ConfigItem.PARENT)
    public HibernateEnversBuildTimeConfigPersistenceUnit defaultPersistenceUnit;

    /**
     * Configuration for additional named persistence units.
     */
    @ConfigDocSection
    @ConfigDocMapKey("persistence-unit-name")
    @ConfigItem(name = ConfigItem.PARENT)
    public Map<String, HibernateEnversBuildTimeConfigPersistenceUnit> persistenceUnits;

    public Map<String, HibernateEnversBuildTimeConfigPersistenceUnit> getAllPersistenceUnitConfigsAsMap() {
        Map<String, HibernateEnversBuildTimeConfigPersistenceUnit> map = new TreeMap<>();
        if (defaultPersistenceUnit != null) {
            map.put(PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME, defaultPersistenceUnit);
        }
        map.putAll(persistenceUnits);
        return map;
    }

    public static String extensionPropertyKey(String radical) {
        return "quarkus.hibernate-envers." + radical;
    }

    public static String persistenceUnitPropertyKey(String persistenceUnitName, String radical) {
        StringBuilder keyBuilder = new StringBuilder("quarkus.hibernate-envers.");
        if (!PersistenceUnitUtil.isDefaultPersistenceUnit(persistenceUnitName)) {
            keyBuilder.append("\"").append(persistenceUnitName).append("\".");
        }
        keyBuilder.append(radical);
        return keyBuilder.toString();
    }
}
