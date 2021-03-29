package io.quarkus.hibernate.reactive.panache.deployment;

import org.jboss.jandex.ClassInfo;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Represents a regular Panache entity class.
 */
public final class PanacheEntityClassBuildItem extends MultiBuildItem {

    private final ClassInfo entityClass;

    public PanacheEntityClassBuildItem(ClassInfo entityClass) {
        this.entityClass = entityClass;
    }

    public ClassInfo get() {
        return entityClass;
    }

}
