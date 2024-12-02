package io.quarkus.mongodb.panache.common.deployment;

import org.jboss.jandex.ClassInfo;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Represents a regular Panache entity class.
 */
public final class PanacheMongoEntityClassBuildItem extends MultiBuildItem {

    private final ClassInfo entityClass;

    public PanacheMongoEntityClassBuildItem(ClassInfo entityClass) {
        this.entityClass = entityClass;
    }

    public ClassInfo get() {
        return entityClass;
    }

}
