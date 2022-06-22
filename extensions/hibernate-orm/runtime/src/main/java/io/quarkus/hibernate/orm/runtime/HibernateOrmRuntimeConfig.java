package io.quarkus.hibernate.orm.runtime;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public class HibernateOrmRuntimeConfig {

    /**
     * Configuration for the default persistence unit.
     */
    @ConfigItem(name = ConfigItem.PARENT)
    public HibernateOrmRuntimeConfigPersistenceUnit defaultPersistenceUnit;

    /**
     * Additional named persistence units.
     */
    @ConfigDocSection
    @ConfigDocMapKey("persistence-unit-name")
    @ConfigItem(name = ConfigItem.PARENT)
    public Map<String, HibernateOrmRuntimeConfigPersistenceUnit> persistenceUnits;

    public static String puPropertyKey(String puName, String radical) {
        String prefix = PersistenceUnitUtil.isDefaultPersistenceUnit(puName)
                ? "quarkus.hibernate-orm."
                : "quarkus.hibernate-orm.\"" + puName + "\".";
        return prefix + radical;
    }

    public boolean isAnyPropertySet() {
        return defaultPersistenceUnit.isAnyPropertySet() ||
                !persistenceUnits.isEmpty();
    }
}
