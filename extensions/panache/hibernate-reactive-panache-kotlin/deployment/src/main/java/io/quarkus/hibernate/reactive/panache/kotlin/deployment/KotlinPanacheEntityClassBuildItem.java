package io.quarkus.hibernate.reactive.panache.kotlin.deployment;

import org.jboss.jandex.ClassInfo;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Represents a regular Panache entity class.
 */
public final class KotlinPanacheEntityClassBuildItem extends MultiBuildItem {

    private final ClassInfo entityClass;

    public KotlinPanacheEntityClassBuildItem(ClassInfo entityClass) {
        this.entityClass = entityClass;
    }

    public ClassInfo get() {
        return entityClass;
    }

}
