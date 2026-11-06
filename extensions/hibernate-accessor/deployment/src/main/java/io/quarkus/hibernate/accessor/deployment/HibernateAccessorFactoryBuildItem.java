package io.quarkus.hibernate.accessor.deployment;

import org.hibernate.accessor.AccessorFactory;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.runtime.RuntimeValue;

/**
 * Publishes the initialized Hibernate accessor factory for other extensions to pass to their runtime services.
 */
public final class HibernateAccessorFactoryBuildItem extends SimpleBuildItem {

    private final RuntimeValue<AccessorFactory> hibernateAccessorFactory;

    public HibernateAccessorFactoryBuildItem(RuntimeValue<AccessorFactory> hibernateAccessorFactory) {
        this.hibernateAccessorFactory = hibernateAccessorFactory;
    }

    /**
     * Returns the factory configured by {@code quarkus.hibernate-accessor.strategy}.
     */
    public RuntimeValue<AccessorFactory> accessorFactory() {
        return hibernateAccessorFactory;
    }
}
