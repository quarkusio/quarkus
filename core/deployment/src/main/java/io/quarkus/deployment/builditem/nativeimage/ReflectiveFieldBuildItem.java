package io.quarkus.deployment.builditem.nativeimage;

import java.lang.reflect.Field;

import org.jboss.jandex.FieldInfo;

import io.quarkus.builder.item.MultiBuildItem;

public final class ReflectiveFieldBuildItem extends MultiBuildItem {

    final String declaringClass;
    final String name;
    final String reason;

    public ReflectiveFieldBuildItem(String reason, FieldInfo field) {
        this(reason, field.declaringClass().name().toString(), field.name());
    }

    public ReflectiveFieldBuildItem(FieldInfo field) {
        this(null, field);
    }

    public ReflectiveFieldBuildItem(Field field) {
        this(null, field);
    }

    public ReflectiveFieldBuildItem(String reason, Field field) {
        this(reason, field.getDeclaringClass().getName(), field.getName());
    }

    public ReflectiveFieldBuildItem(String reason, String declaringClass, String fieldName) {
        this.reason = reason;
        this.name = fieldName;
        this.declaringClass = declaringClass;
    }

    public String getDeclaringClass() {
        return declaringClass;
    }

    public String getName() {
        return name;
    }

    public String getReason() {
        return reason;
    }
}
