package io.quarkus.deployment.builditem.nativeimage;

import java.lang.reflect.Field;

import org.jboss.jandex.FieldInfo;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * JNI access registration fine-grained to single fields
 * for a given class.
 */
public final class JniRuntimeAccessFieldBuildItem extends MultiBuildItem {

    final String declaringClass;
    final String name;

    public JniRuntimeAccessFieldBuildItem(FieldInfo field) {
        this.name = field.name();
        this.declaringClass = field.declaringClass().name().toString();
    }

    public JniRuntimeAccessFieldBuildItem(Field field) {
        this.name = field.getName();
        this.declaringClass = field.getDeclaringClass().getName();
    }

    public JniRuntimeAccessFieldBuildItem(String declaringClass, String name) {
        this.name = name;
        this.declaringClass = declaringClass;
    }

    public String getDeclaringClass() {
        return declaringClass;
    }

    public String getName() {
        return name;
    }
}
