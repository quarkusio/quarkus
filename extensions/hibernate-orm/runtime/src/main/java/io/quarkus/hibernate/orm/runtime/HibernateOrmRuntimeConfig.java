package io.quarkus.hibernate.orm.runtime;

import java.util.Map;
import java.util.TreeMap;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefaults;
import io.smallrye.config.WithParentName;

@ConfigMapping(prefix = "quarkus.hibernate-orm")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface HibernateOrmRuntimeConfig {

    /**
     * Configuration for the default persistence unit.
     */
    @WithParentName
    HibernateOrmRuntimeConfigPersistenceUnit defaultPersistenceUnit();

    /**
     * Additional named persistence units.
     */
    @ConfigDocSection
    @WithParentName
    @WithDefaults
    @ConfigDocMapKey("persistence-unit-name")
    Map<String, HibernateOrmRuntimeConfigPersistenceUnit> namedPersistenceUnits();

    default Map<String, HibernateOrmRuntimeConfigPersistenceUnit> getAllPersistenceUnitConfigsAsMap() {
        Map<String, HibernateOrmRuntimeConfigPersistenceUnit> map = new TreeMap<>();
        map.put(PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME, defaultPersistenceUnit());
        map.putAll(namedPersistenceUnits());
        return map;
    }

    default HibernateOrmRuntimeConfigPersistenceUnit getPersistenceUnitOrDefault(String name) {
        if (PersistenceUnitUtil.isDefaultPersistenceUnit(name)) {
            // This is never null
            return defaultPersistenceUnit();
        } else {
            // See @WithDefaults on namedPersistenceUnits(): this never returns null
            return namedPersistenceUnits().get(name);
        }
    }

    static String extensionPropertyKey(String radical) {
        return "quarkus.hibernate-orm." + radical;
    }

    static String puPropertyKey(String puName, String radical) {
        String prefix = PersistenceUnitUtil.isDefaultPersistenceUnit(puName)
                ? "quarkus.hibernate-orm."
                : "quarkus.hibernate-orm.\"" + puName + "\".";
        return prefix + radical;
    }
}
