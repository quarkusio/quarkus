package io.quarkus.hibernate.orm.runtime.schema;

import java.util.Locale;
import java.util.Optional;

import jakarta.persistence.PersistenceException;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.tool.schema.Action;
import org.jboss.logging.Logger;

import io.quarkus.hibernate.orm.runtime.HibernateOrmRuntimeConfig;
import io.quarkus.hibernate.orm.runtime.HibernateOrmRuntimeConfigPersistenceUnit;
import io.quarkus.hibernate.orm.runtime.HibernateOrmRuntimeConfigPersistenceUnit.DataManagementStrategy;
import io.quarkus.hibernate.orm.runtime.HibernateOrmRuntimeConfigPersistenceUnit.HibernateGenerationStrategy;
import io.quarkus.hibernate.orm.runtime.RuntimeSettings;
import io.quarkus.runtime.LaunchMode;

/**
 * Support for the SQL init scripts configured at build time
 * ({@code quarkus.hibernate-orm.data-management.init-script},
 * {@code quarkus.hibernate-orm.schema-management.init-script}
 * and the deprecated {@code quarkus.hibernate-orm.sql-load-script}).
 */
public final class InitScriptSupport {

    private static final Logger LOG = Logger.getLogger(InitScriptSupport.class);

    /**
     * Persistence unit property set to {@code true} at build time when the data init script
     * comes from the deprecated {@code quarkus.hibernate-orm.sql-load-script} property.
     * Such scripts keep their historical behavior: they are only executed
     * when Hibernate ORM creates the schema.
     */
    public static final String LEGACY_SQL_LOAD_SCRIPT = "hibernate.quarkus.legacy_sql_load_script";

    /**
     * Value of {@link AvailableSettings#JAKARTA_HBM2DDL_CREATE_SOURCE} making Hibernate ORM
     * execute the schema init script right after the schema generated from the entity model.
     */
    public static final String CREATE_SOURCE_METADATA_THEN_SCRIPT = "metadata-then-script";

    private InitScriptSupport() {
    }

    /**
     * Applies the data management configuration of a persistence unit to its runtime settings.
     * <p>
     * The data init script (if any) is recorded in the {@link AvailableSettings#JAKARTA_HBM2DDL_LOAD_SCRIPT_SOURCE}
     * setting, which Hibernate ORM only executes when it creates the schema.
     * This method makes sure the script is also executed with schema management strategies
     * that do not create the schema, unless {@code quarkus.hibernate-orm.data-management.strategy} is {@code none}.
     *
     * @return {@code true} if the data init script must be executed once the session factory is created
     *         (see {@link io.quarkus.hibernate.orm.runtime.observers.SessionFactoryObserverForDataPopulation}),
     *         {@code false} if it is executed by Hibernate ORM as part of the schema management action,
     *         or not executed at all.
     */
    public static boolean configureDataManagement(String persistenceUnitName,
            HibernateOrmRuntimeConfigPersistenceUnit persistenceUnitConfig,
            HibernateGenerationStrategy schemaManagementStrategy,
            RuntimeSettings.Builder runtimeSettingsBuilder) {
        boolean legacy = Boolean.parseBoolean(String.valueOf(runtimeSettingsBuilder.get(LEGACY_SQL_LOAD_SCRIPT)));
        // Quarkus-only marker, not a Hibernate ORM setting
        runtimeSettingsBuilder.put(LEGACY_SQL_LOAD_SCRIPT, null);

        if (runtimeSettingsBuilder.get(AvailableSettings.JAKARTA_HBM2DDL_LOAD_SCRIPT_SOURCE) == null) {
            // No data init script
            return false;
        }
        Optional<DataManagementStrategy> configuredStrategy = persistenceUnitConfig.dataManagement().strategy();
        DataManagementStrategy strategy = configuredStrategy
                .orElse(LaunchMode.current().isDevOrTest() ? DataManagementStrategy.CREATE : DataManagementStrategy.NONE);

        if (legacy) {
            // Scripts set through the deprecated 'quarkus.hibernate-orm.sql-load-script'
            // are only executed when Hibernate ORM creates the schema, and regardless of the launch mode;
            // only an explicit 'none' strategy disables them.
            if (configuredStrategy.isPresent() && DataManagementStrategy.NONE.equals(strategy)) {
                runtimeSettingsBuilder.put(AvailableSettings.JAKARTA_HBM2DDL_LOAD_SCRIPT_SOURCE, null);
            }
            return false;
        }

        if (DataManagementStrategy.NONE.equals(strategy)) {
            if (configuredStrategy.isEmpty()) {
                LOG.warnf("Persistence unit '%s': not executing the data init script since `%s` defaults to `none`"
                        + " outside of dev and test modes; set it to `create` to execute the script.",
                        persistenceUnitName,
                        HibernateOrmRuntimeConfig.puPropertyKey(persistenceUnitName, "data-management.strategy"));
            }
            runtimeSettingsBuilder.put(AvailableSettings.JAKARTA_HBM2DDL_LOAD_SCRIPT_SOURCE, null);
            return false;
        }

        if (persistenceUnitConfig.database().startOffline()) {
            if (configuredStrategy.isPresent()) {
                throw new PersistenceException(String.format(Locale.ROOT,
                        "When using offline mode with `%s=true`, the data management strategy `%s` must be unset or set to `none`",
                        HibernateOrmRuntimeConfig.puPropertyKey(persistenceUnitName, "database.start-offline"),
                        HibernateOrmRuntimeConfig.puPropertyKey(persistenceUnitName, "data-management.strategy")));
            }
            LOG.warnf(
                    "Persistence unit '%s': not executing the data init script since Hibernate ORM starts offline (`%s=true`)",
                    persistenceUnitName,
                    HibernateOrmRuntimeConfig.puPropertyKey(persistenceUnitName, "database.start-offline"));
            runtimeSettingsBuilder.put(AvailableSettings.JAKARTA_HBM2DDL_LOAD_SCRIPT_SOURCE, null);
            return false;
        }

        switch (schemaManagementStrategy) {
            case CREATE:
            case DROP_AND_CREATE:
                // Executed by Hibernate ORM right after creating the schema
                return false;
            case NONE:
                // Nothing else to do on the schema, just execute the script.
                // "populate" is not a JPA-standard action name, but Hibernate ORM accepts it for this setting.
                runtimeSettingsBuilder.put(AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION, Action.ACTION_POPULATE);
                return false;
            case UPDATE:
            case VALIDATE:
                // Executed once the schema has been updated/validated
                return true;
            case DROP:
                LOG.debugf("Persistence unit '%s': not executing the data init script since the schema is being dropped",
                        persistenceUnitName);
                runtimeSettingsBuilder.put(AvailableSettings.JAKARTA_HBM2DDL_LOAD_SCRIPT_SOURCE, null);
                return false;
            default:
                throw new IllegalStateException("Unexpected schema management strategy: " + schemaManagementStrategy);
        }
    }
}
