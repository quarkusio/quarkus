package io.quarkus.data.hibernate.deployment;

import org.jboss.jandex.ClassInfo;

import io.quarkus.builder.item.MultiBuildItem;

//FIXME: duplicate with ORM and probably HR
/**
 * Represents a regular Quarkus Data entity class.
 */
public final class QuarkusDataEntityClassBuildItem extends MultiBuildItem {

    private final ClassInfo entityClass;

    public QuarkusDataEntityClassBuildItem(ClassInfo entityClass) {
        this.entityClass = entityClass;
    }

    public ClassInfo get() {
        return entityClass;
    }

}
