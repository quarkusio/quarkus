package io.quarkus.hibernate.orm.deployment;

import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot
public class HibernateOrmConfig {

    /**
     * Configuration for the default persistence unit.
     */
    @ConfigItem(name = ConfigItem.PARENT)
    public HibernateOrmConfigPersistenceUnit defaultPersistenceUnit;

    /**
     * Additional named persistence units.
     */
    @ConfigDocSection
    @ConfigDocMapKey("persistence-unit-name")
    @ConfigItem(name = ConfigItem.PARENT)
    public Map<String, HibernateOrmConfigPersistenceUnit> persistenceUnits;

    /**
     * Logging configuration.
     */
    @ConfigItem
    @ConfigDocSection
    public HibernateOrmConfigLog log;

    /**
     * Whether statistics collection is enabled. If 'metrics.enabled' is true, then the default here is
     * considered true, otherwise the default is false.
     */
    @ConfigItem
    public Optional<Boolean> statistics;

    /**
     * Whether or not metrics are published if a metrics extension is enabled.
     */
    @ConfigItem(name = "metrics.enabled")
    public boolean metricsEnabled;

    // TODO MULTI-PUS: the multi-tenant configuration will need some profound changes but we can do that later

    /**
     * Defines the method for multi-tenancy (DATABASE, NONE, SCHEMA). The complete list of allowed values is available in the
     * https://docs.jboss.org/hibernate/stable/orm/javadocs/org/hibernate/MultiTenancyStrategy.html[Hibernate ORM JavaDoc].
     * The type DISCRIMINATOR is currently not supported. The default value is NONE (no multi-tenancy).
     *
     * @asciidoclet
     */
    @ConfigItem
    public Optional<String> multitenant;

    /**
     * Defines the name of the data source to use in case of SCHEMA approach. The default data source will be used if not set.
     *
     * @asciidoclet
     */
    @ConfigItem
    public Optional<String> multitenantSchemaDatasource;

    public boolean isAnyPropertySet() {
        return defaultPersistenceUnit.isAnyPropertySet() ||
                !persistenceUnits.isEmpty() ||
                log.isAnyPropertySet() ||
                statistics.isPresent() ||
                metricsEnabled ||
                multitenant.isPresent() ||
                multitenantSchemaDatasource.isPresent();
    }

    @ConfigGroup
    public static class HibernateOrmConfigLog {

        /**
         * Logs SQL bind parameter.
         * <p>
         * Setting it to true is obviously not recommended in production.
         */
        @ConfigItem
        public boolean bindParam;

        public boolean isAnyPropertySet() {
            return bindParam;
        }
    }
}
