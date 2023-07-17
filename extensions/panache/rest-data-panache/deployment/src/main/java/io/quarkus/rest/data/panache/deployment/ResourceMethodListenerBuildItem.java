package io.quarkus.rest.data.panache.deployment;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.Type;

import io.quarkus.builder.item.MultiBuildItem;

public final class ResourceMethodListenerBuildItem extends MultiBuildItem {

    private final ClassInfo classInfo;
    private final Type entityType;

    public ResourceMethodListenerBuildItem(ClassInfo classInfo, Type entityType) {
        this.classInfo = classInfo;
        this.entityType = entityType;
    }

    public ClassInfo getClassInfo() {
        return classInfo;
    }

    public Type getEntityType() {
        return entityType;
    }
}
