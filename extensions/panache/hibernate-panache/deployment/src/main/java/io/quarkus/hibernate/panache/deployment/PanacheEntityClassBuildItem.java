package io.quarkus.hibernate.panache.deployment;

import org.jboss.jandex.ClassInfo;

import io.quarkus.builder.item.MultiBuildItem;

//FIXME: duplicate with ORM and probably HR
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
