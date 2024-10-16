package io.quarkus.hibernate.envers;

import java.util.Map;

import io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithParentName;
import io.smallrye.config.WithUnnamedKey;

@ConfigMapping(prefix = "quarkus.hibernate-envers")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface HibernateEnversBuildTimeConfig {
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
    @WithDefault("true")
    boolean enabled();

    /**
     * Configuration for persistence units.
     */
    @WithParentName
    @WithUnnamedKey(PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME)
    @ConfigDocMapKey("persistence-unit-name")
    Map<String, HibernateEnversBuildTimeConfigPersistenceUnit> persistenceUnits();

    static String extensionPropertyKey(String radical) {
        return "quarkus.hibernate-envers." + radical;
    }

    static String persistenceUnitPropertyKey(String persistenceUnitName, String radical) {
        StringBuilder keyBuilder = new StringBuilder("quarkus.hibernate-envers.");
        if (!PersistenceUnitUtil.isDefaultPersistenceUnit(persistenceUnitName)) {
            keyBuilder.append("\"").append(persistenceUnitName).append("\".");
        }
        keyBuilder.append(radical);
        return keyBuilder.toString();
    }
}
