package io.quarkus.hibernate.orm.deployment.spi;

import java.util.Objects;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.hibernate.orm.runtime.spi.HibernateOrmIntegrationStaticInitListener;

/**
 * Registers a third-party integration that participates in the Hibernate ORM static-init bootstrap.
 * <p>
 * Produced by extensions that need to hook into the Hibernate ORM bootstrap at build time
 * (e.g. Hibernate Envers, Hibernate Search, Hibernate Reactive).
 * Each item targets a specific persistence unit and may carry a
 * {@link HibernateOrmIntegrationStaticInitListener} to contribute boot properties
 * and react to metadata initialization.
 * <p>
 * Consumed during the {@code STATIC_INIT} build step that builds
 * {@code QuarkusPersistenceUnitDefinition}s and records metadata initialization.
 *
 * @see HibernateOrmIntegrationStaticInitListener
 * @see HibernateOrmIntegrationRuntimeConfiguredBuildItem
 */
public final class HibernateOrmIntegrationStaticConfiguredBuildItem extends MultiBuildItem {

    private final String integrationName;
    private final String persistenceUnitName;
    private final HibernateOrmIntegrationStaticInitListener initListener;
    private final boolean xmlMappingRequired;

    private HibernateOrmIntegrationStaticConfiguredBuildItem(Builder builder) {
        this.integrationName = builder.integrationName;
        this.persistenceUnitName = builder.persistenceUnitName;
        this.initListener = builder.initListener;
        this.xmlMappingRequired = builder.xmlMappingRequired;
    }

    /**
     * @param integrationName a unique identifier for the integration (e.g. {@code "hibernate-envers"})
     * @param persistenceUnitName the name of the persistence unit this integration targets
     */
    public static Builder builder(String integrationName, String persistenceUnitName) {
        return new Builder(integrationName, persistenceUnitName);
    }

    @Override
    public String toString() {
        return HibernateOrmIntegrationStaticConfiguredBuildItem.class.getSimpleName() + " [" + integrationName + "]";
    }

    public String getIntegrationName() {
        return integrationName;
    }

    public String getPersistenceUnitName() {
        return persistenceUnitName;
    }

    public HibernateOrmIntegrationStaticInitListener getInitListener() {
        return initListener;
    }

    public boolean isXmlMappingRequired() {
        return xmlMappingRequired;
    }

    public static final class Builder {

        private final String integrationName;
        private final String persistenceUnitName;
        private HibernateOrmIntegrationStaticInitListener initListener;
        private boolean xmlMappingRequired = false;

        private Builder(String integrationName, String persistenceUnitName) {
            this.integrationName = Objects.requireNonNull(integrationName, "integrationName must not be null");
            this.persistenceUnitName = Objects.requireNonNull(persistenceUnitName, "persistenceUnitName must not be null");
        }

        /**
         * Sets a listener that will be called during static init to contribute boot properties
         * and react to metadata initialization.
         */
        public Builder initListener(HibernateOrmIntegrationStaticInitListener initListener) {
            this.initListener = initListener;
            return this;
        }

        /**
         * Indicates that this integration requires XML mapping to be enabled for its persistence unit.
         */
        public Builder xmlMappingRequired(boolean xmlMappingRequired) {
            this.xmlMappingRequired = xmlMappingRequired;
            return this;
        }

        public HibernateOrmIntegrationStaticConfiguredBuildItem build() {
            return new HibernateOrmIntegrationStaticConfiguredBuildItem(this);
        }
    }
}
