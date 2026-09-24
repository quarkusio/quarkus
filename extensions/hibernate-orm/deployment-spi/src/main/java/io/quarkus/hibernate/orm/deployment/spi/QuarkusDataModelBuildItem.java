package io.quarkus.hibernate.orm.deployment.spi;

import java.util.Objects;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Item produced by Quarkus Data that needs to be registered as a managed class.
 * The persistence unit is not specified here; it is resolved from the enclosing
 * entity's persistence unit by {@code HibernateOrmProcessor.buildJpaModelPerPersistenceUnit}.
 */
public final class QuarkusDataModelBuildItem extends MultiBuildItem {

    private final String className;
    private final String enclosingEntityClassName;

    public QuarkusDataModelBuildItem(String className, String enclosingEntityClassName) {
        Objects.requireNonNull(className);
        Objects.requireNonNull(enclosingEntityClassName);
        this.className = className;
        this.enclosingEntityClassName = enclosingEntityClassName;
    }

    public String getClassName() {
        return className;
    }

    public String getEnclosingEntityClassName() {
        return enclosingEntityClassName;
    }
}
