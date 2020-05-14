package io.quarkus.panache.rest.common.deployment;

import org.jboss.jandex.ClassInfo;

import io.quarkus.builder.item.MultiBuildItem;

public final class PanacheCrudResourceInfo extends MultiBuildItem {

    private final ClassInfo resourceClassInfo;

    private final DataAccessImplementor dataAccessImplementor;

    private final IdFieldPredicate idFieldPredicate;

    private final String idClassName;

    private final String entityClassName;

    public PanacheCrudResourceInfo(ClassInfo resourceClassInfo, DataAccessImplementor dataAccessImplementor,
            IdFieldPredicate idFieldPredicate, String idClassName, String entityClassName) {
        this.resourceClassInfo = resourceClassInfo;
        this.dataAccessImplementor = dataAccessImplementor;
        this.idFieldPredicate = idFieldPredicate;
        this.idClassName = idClassName;
        this.entityClassName = entityClassName;
    }

    public ClassInfo getResourceClassInfo() {
        return resourceClassInfo;
    }

    public DataAccessImplementor getDataAccessImplementor() {
        return dataAccessImplementor;
    }

    public IdFieldPredicate getIdFieldPredicate() {
        return idFieldPredicate;
    }

    public String getIdClassName() {
        return idClassName;
    }

    public String getEntityClassName() {
        return entityClassName;
    }
}
