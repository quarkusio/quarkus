package io.quarkus.hibernate.orm.runtime;

import java.util.List;
import java.util.Map;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithDefaults;
import io.smallrye.config.WithName;
import io.smallrye.config.WithParentName;
import io.smallrye.config.WithUnnamedKey;

@ConfigMapping(prefix = "quarkus.hibernate-orm")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface HibernateOrmRuntimeConfig {

    /**
     * Configuration for persistence units.
     */
    @WithParentName
    @WithUnnamedKey(PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME)
    @WithDefaults
    @ConfigDocMapKey("persistence-unit-name")
    Map<String, HibernateOrmRuntimeConfigPersistenceUnit> persistenceUnits();

    /**
     * Enable or disable access to a Hibernate ORM `EntityManager`/`Session`/`StatelessSession`
     * *when no transaction is active* but a request scope is.
     *
     * When enabled, the corresponding sessions will be read-only.
     *
     * Defaults to enabled for backwards compatibility, but disabling this is recommended,
     * to avoid inconsistent results caused by queries running outside of transactions.
     */
    @WithName("request-scoped.enabled")
    @WithDefault("true")
    boolean requestScopedSessionEnabled();

    /**
     * Enable or disable write operations on a Hibernate ORM `StatelessSession`
     * *when no transaction is active* but a request scope is.
     *
     * When disabled, no mutation operations will be allowed outside of active transactions.
     *
     * Defaults to 'false' but, for backwards compatibility, it can be set to 'true' if you have valid reasons
     * to not use transactions.
     */
    @WithName("request-scoped.stateless-session.allow-write")
    @WithDefault("false")
    boolean requestScopedStatelessSessionAllowWrite();

    static String extensionPropertyKey(String radical) {
        return "quarkus.hibernate-orm." + radical;
    }

    static String puPropertyKey(String puName, String radical) {
        String prefix = PersistenceUnitUtil.isDefaultPersistenceUnit(puName)
                ? "quarkus.hibernate-orm."
                : "quarkus.hibernate-orm.\"" + puName + "\".";
        return prefix + radical;
    }

    /**
     * Returns both the quoted and unquoted property key variants for a persistence unit property.
     * <p>
     * SmallRye Config treats {@code quarkus.hibernate-orm."pu-name".x} and
     * {@code quarkus.hibernate-orm.pu-name.x} as two completely independent properties
     * with no cross-resolution: setting one does not make the other visible.
     * Both forms must therefore be set when producing config defaults programmatically,
     * and both must be checked when looking up user-supplied config.
     */
    static List<String> puPropertyKeys(String puName, String radical) {
        if (PersistenceUnitUtil.isDefaultPersistenceUnit(puName)) {
            return List.of("quarkus.hibernate-orm." + radical);
        }
        return List.of(
                "quarkus.hibernate-orm.\"" + puName + "\"." + radical,
                "quarkus.hibernate-orm." + puName + "." + radical);
    }
}
