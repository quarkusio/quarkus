package io.quarkus.hibernate.orm.deployment.spi;

import java.util.Objects;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.hibernate.orm.runtime.spi.HibernateOrmIntegrationRuntimeInitListener;

/**
 * Registers a third-party integration that participates in the Hibernate ORM runtime-init bootstrap.
 * <p>
 * Produced by extensions that need to hook into the Hibernate ORM bootstrap at runtime
 * (e.g. Hibernate Search, Hibernate Reactive).
 * Each item targets a specific persistence unit and may carry a
 * {@link HibernateOrmIntegrationRuntimeInitListener} to contribute runtime properties
 * and service initiators.
 * <p>
 * Consumed during the {@code RUNTIME_INIT} build step that sets up the persistence provider
 * and starts persistence units.
 *
 * @see HibernateOrmIntegrationRuntimeInitListener
 * @see HibernateOrmIntegrationStaticConfiguredBuildItem
 */
public final class HibernateOrmIntegrationRuntimeConfiguredBuildItem extends MultiBuildItem {

    private final String integrationName;
    private final String persistenceUnitName;
    private final HibernateOrmIntegrationRuntimeInitListener initListener;

    private HibernateOrmIntegrationRuntimeConfiguredBuildItem(Builder builder) {
        this.integrationName = builder.integrationName;
        this.persistenceUnitName = builder.persistenceUnitName;
        this.initListener = builder.initListener;
    }

    /**
     * @param integrationName a unique identifier for the integration (e.g. {@code "hibernate-search-elasticsearch"})
     * @param persistenceUnitName the name of the persistence unit this integration targets
     */
    public static Builder builder(String integrationName, String persistenceUnitName) {
        return new Builder(integrationName, persistenceUnitName);
    }

    @Override
    public String toString() {
        return HibernateOrmIntegrationRuntimeConfiguredBuildItem.class.getSimpleName() + " [" + integrationName + "]";
    }

    public String getIntegrationName() {
        return integrationName;
    }

    public String getPersistenceUnitName() {
        return persistenceUnitName;
    }

    public HibernateOrmIntegrationRuntimeInitListener getInitListener() {
        return initListener;
    }

    public static final class Builder {

        private final String integrationName;
        private final String persistenceUnitName;
        private HibernateOrmIntegrationRuntimeInitListener initListener;

        private Builder(String integrationName, String persistenceUnitName) {
            this.integrationName = Objects.requireNonNull(integrationName, "integrationName must not be null");
            this.persistenceUnitName = Objects.requireNonNull(persistenceUnitName, "persistenceUnitName must not be null");
        }

        /**
         * Sets a listener that will be called during runtime init to contribute runtime properties
         * and service initiators.
         */
        public Builder initListener(HibernateOrmIntegrationRuntimeInitListener initListener) {
            this.initListener = initListener;
            return this;
        }

        public HibernateOrmIntegrationRuntimeConfiguredBuildItem build() {
            return new HibernateOrmIntegrationRuntimeConfiguredBuildItem(this);
        }
    }
}
