package io.quarkus.hibernate.orm.deployment;

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
            false);
    private final boolean shouldGenerateOne;

    private ImpliedBlockingPersistenceUnitTypeBuildItem(boolean shouldGenerateOne) {
        this.shouldGenerateOne = shouldGenerateOne;
    }

    public static ImpliedBlockingPersistenceUnitTypeBuildItem none() {
        return NONE;
    }

    public static ImpliedBlockingPersistenceUnitTypeBuildItem generateImpliedPersistenceUnit() {
        return new ImpliedBlockingPersistenceUnitTypeBuildItem(true);
    }

    public boolean shouldGenerateImpliedBlockingPersistenceUnit() {
        return shouldGenerateOne;
    }
}
