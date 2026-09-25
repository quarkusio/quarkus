package io.quarkus.hibernate.orm.runtime.schema;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
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
import io.quarkus.runtime.configuration.ConfigurationException;

/**
 * Support for the SQL init scripts configured at build time
 * ({@code quarkus.hibernate-orm.data-management.init-script},
 * {@code quarkus.hibernate-orm.schema-management.init-script}
 * and the deprecated {@code quarkus.hibernate-orm.sql-load-script}).
 */
public final class InitScriptSupport {

    private static final Logger LOG = Logger.getLogger(InitScriptSupport.class);

    /**
     * Persistence unit property set to {@code true} at build time when the deprecated
     * {@code quarkus.hibernate-orm.sql-load-script} property is used.
     * Such scripts keep their historical behavior: they are only executed
     * when Hibernate ORM creates the schema, and the data management strategy does not apply to them.
     */
    public static final String LEGACY_SQL_LOAD_SCRIPT = "hibernate.quarkus.legacy_sql_load_script";

    /**
     * Persistence unit property set to {@code false} when the data init script must not be executed
     * by the schema management that Quarkus triggers itself (on startup, and on reset from the Dev UI).
     * The script stays in the settings so that explicit {@code SchemaManager} calls can still execute it,
     * see {@link #schemaManagementSettings(Map)}.
     */
    public static final String DATA_INIT_SCRIPT_ON_START = "hibernate.quarkus.data_init_script.on_start";

    /**
     * Value of {@link AvailableSettings#JAKARTA_HBM2DDL_CREATE_SOURCE} making Hibernate ORM
     * execute the schema init script right after the schema generated from the entity model.
     */
    public static final String CREATE_SOURCE_METADATA_THEN_SCRIPT = "metadata-then-script";

    private static final String SQL_LOAD_SCRIPT_PROPERTY = "sql-load-script";
    private static final String DATA_INIT_SCRIPT_PROPERTY = "data-management.init-script";
    private static final String DATA_MANAGEMENT_STRATEGY_PROPERTY = "data-management.strategy";

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
        Optional<DataManagementStrategy> configuredStrategy = persistenceUnitConfig.dataManagement().strategy();

        if (legacy) {
            if (configuredStrategy.isPresent()) {
                throw new ConfigurationException(String.format(Locale.ROOT,
                        "'%s' is deprecated and cannot be used together with '%s'. Remove it and use '%s' instead.",
                        HibernateOrmRuntimeConfig.puPropertyKey(persistenceUnitName, SQL_LOAD_SCRIPT_PROPERTY),
                        HibernateOrmRuntimeConfig.puPropertyKey(persistenceUnitName, DATA_MANAGEMENT_STRATEGY_PROPERTY),
                        HibernateOrmRuntimeConfig.puPropertyKey(persistenceUnitName, DATA_INIT_SCRIPT_PROPERTY)));
            }
            // Scripts set through the deprecated 'quarkus.hibernate-orm.sql-load-script' keep their historical behavior:
            // Hibernate ORM executes them when it creates the schema, regardless of the launch mode.
            return false;
        }

        if (runtimeSettingsBuilder.get(AvailableSettings.JAKARTA_HBM2DDL_LOAD_SCRIPT_SOURCE) == null) {
            // No data init script
            return false;
        }

        DataManagementStrategy strategy = configuredStrategy.orElseGet(InitScriptSupport::defaultDataManagementStrategy);
        if (DataManagementStrategy.NONE.equals(strategy)) {
            LOG.debugf("Persistence unit '%s': not executing the data init script on start (`%s` is `none`)",
                    persistenceUnitName,
                    HibernateOrmRuntimeConfig.puPropertyKey(persistenceUnitName, DATA_MANAGEMENT_STRATEGY_PROPERTY));
            // The script stays in the settings so that it remains available to explicit SchemaManager calls
            // (populate(), truncate()); only the schema management triggered by Quarkus ignores it.
            runtimeSettingsBuilder.put(DATA_INIT_SCRIPT_ON_START, false);
            return false;
        }

        if (persistenceUnitConfig.database().startOffline()) {
            if (configuredStrategy.isPresent()) {
                throw new PersistenceException(String.format(Locale.ROOT,
                        "When using offline mode with `%s=true`, the data management strategy `%s` must be unset or set to `none`",
                        HibernateOrmRuntimeConfig.puPropertyKey(persistenceUnitName, "database.start-offline"),
                        HibernateOrmRuntimeConfig.puPropertyKey(persistenceUnitName, DATA_MANAGEMENT_STRATEGY_PROPERTY)));
            }
            LOG.warnf(
                    "Persistence unit '%s': not executing the data init script since Hibernate ORM starts offline (`%s=true`)",
                    persistenceUnitName,
                    HibernateOrmRuntimeConfig.puPropertyKey(persistenceUnitName, "database.start-offline"));
            // As with the "none" strategy, the script stays available to explicit SchemaManager calls
            runtimeSettingsBuilder.put(DATA_INIT_SCRIPT_ON_START, false);
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
                runtimeSettingsBuilder.put(DATA_INIT_SCRIPT_ON_START, false);
                return false;
            default:
                throw new IllegalStateException("Unexpected schema management strategy: " + schemaManagementStrategy);
        }
    }

    /**
     * The settings to use for the schema management that Quarkus triggers itself:
     * on startup, and on reset from the Dev UI.
     * <p>
     * Hibernate ORM executes the data init script as part of schema creation, truncation and population alike,
     * reading it from the same settings.
     * When the script must not be executed on start, it still has to stay in the session factory settings
     * for explicit {@code SchemaManager} calls ({@code populate()}, {@code truncate()}) to see it,
     * so it is only removed from the copy of the settings used for the schema management triggered by Quarkus.
     */
    public static Map<String, Object> schemaManagementSettings(Map<String, Object> settings) {
        Object onStart = settings.get(DATA_INIT_SCRIPT_ON_START);
        if (onStart == null || Boolean.parseBoolean(String.valueOf(onStart))) {
            return settings;
        }
        Map<String, Object> copy = new HashMap<>(settings);
        // The only setting Quarkus uses for the data init script
        copy.remove(AvailableSettings.JAKARTA_HBM2DDL_LOAD_SCRIPT_SOURCE);
        return copy;
    }

    /**
     * The data init script is executed by default in dev and test modes only.
     * It is not executed by default in other modes (e.g. in production), even when Hibernate ORM creates the schema:
     * loading data there is an explicit choice, like creating the schema is.
     */
    public static DataManagementStrategy defaultDataManagementStrategy() {
        return LaunchMode.current().isDevOrTest() ? DataManagementStrategy.CREATE : DataManagementStrategy.NONE;
    }
}
