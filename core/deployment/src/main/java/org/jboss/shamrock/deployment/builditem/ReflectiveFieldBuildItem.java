package org.jboss.shamrock.deployment.builditem;

import java.lang.reflect.Field;

import org.jboss.builder.item.MultiBuildItem;
import org.jboss.jandex.FieldInfo;

public final class ReflectiveFieldBuildItem extends MultiBuildItem {

    final String declaringClass;
    final String name;

    public ReflectiveFieldBuildItem(FieldInfo field) {
        this.name = field.name();
        this.declaringClass = field.declaringClass().name().toString();
    }

    public ReflectiveFieldBuildItem(Field field) {
        this.name = field.getName();
        this.declaringClass = field.getDeclaringClass().getName();
    }

    public String getDeclaringClass() {
        return declaringClass;
    }

    public String getName() {
        return name;
    }
}
