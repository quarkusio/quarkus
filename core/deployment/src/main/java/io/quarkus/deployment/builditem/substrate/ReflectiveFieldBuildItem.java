package io.quarkus.deployment.builditem.substrate;

import java.lang.reflect.Field;

import org.jboss.jandex.FieldInfo;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * @deprecated Use {@link io.quarkus.deployment.builditem.nativeimage.ReflectiveFieldBuildItem ReflectiveFieldBuildItem}
 *             instead.
 */
@Deprecated
public final class ReflectiveFieldBuildItem extends MultiBuildItem {

    final String declaringClass;
    final String name;
    private final FieldInfo fieldInfo;
    private final Field field;

    public ReflectiveFieldBuildItem(FieldInfo field) {
        this.name = field.name();
        this.declaringClass = field.declaringClass().name().toString();
        this.fieldInfo = field;
        this.field = null;
    }

    public ReflectiveFieldBuildItem(Field field) {
        this.name = field.getName();
        this.declaringClass = field.getDeclaringClass().getName();
        this.fieldInfo = null;
        this.field = field;
    }

    public String getDeclaringClass() {
        return declaringClass;
    }

    public String getName() {
        return name;
    }

    public FieldInfo getFieldInfo() {
        return fieldInfo;
    }

    public Field getField() {
        return field;
    }
}
