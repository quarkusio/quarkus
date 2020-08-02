package io.quarkus.hibernate.orm.deployment;

import io.quarkus.agroal.deployment.JdbcDataSourceBuildItem;
import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Quarkus attempts to automatically define a persistence unit when the Hibernate ORM
 * extension is enabled, a default datasource is defined, and there are mapped entities.
 * This build item represents the decision about creating such an implied persistence
 * unit; it's modelled as a BuildItem so that other extensions can be aware of such
 * a persistence unit being defined (e.g. Hibernate Reactive needs to know).
 */
public final class ImpliedBlockingPersistenceUnitTypeBuildItem extends SimpleBuildItem {

    private static final ImpliedBlockingPersistenceUnitTypeBuildItem NONE = new ImpliedBlockingPersistenceUnitTypeBuildItem(
            false, null);
    private final boolean shouldGenerateOne;
    private final JdbcDataSourceBuildItem datasourceConfig;

    private ImpliedBlockingPersistenceUnitTypeBuildItem(boolean shouldGenerateOne, JdbcDataSourceBuildItem datasourceConfig) {
        this.shouldGenerateOne = shouldGenerateOne;
        this.datasourceConfig = datasourceConfig;
    }

    public static ImpliedBlockingPersistenceUnitTypeBuildItem none() {
        return NONE;
    }

    public static ImpliedBlockingPersistenceUnitTypeBuildItem generateImpliedPersistenceUnit(
            JdbcDataSourceBuildItem datasourceConfig) {
        return new ImpliedBlockingPersistenceUnitTypeBuildItem(true, datasourceConfig);
    }

    public boolean shouldGenerateImpliedBlockingPersistenceUnit() {
        return shouldGenerateOne;
    }

    public JdbcDataSourceBuildItem getDatasourceBuildTimeConfiguration() {
        return datasourceConfig;
    }
}
